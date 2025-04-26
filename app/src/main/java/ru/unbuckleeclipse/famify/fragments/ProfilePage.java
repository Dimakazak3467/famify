package ru.unbuckleeclipse.famify.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;

import ru.unbuckleeclipse.famify.LoginActivity;
import ru.unbuckleeclipse.famify.R;

public class ProfilePage extends Fragment {

    private EditText familyCodeInput, customNameInput;
    private Button joinFamilyButton, createFamilyButton, signOutButton;
    private TextView currentFamilyCode, nameTextView, emailTextView;
    private Spinner nameSourceSpinner;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        nameTextView = view.findViewById(R.id.profile_name);
        emailTextView = view.findViewById(R.id.profile_email);
        signOutButton = view.findViewById(R.id.sign_out_button);

        familyCodeInput = view.findViewById(R.id.family_code_input);
        joinFamilyButton = view.findViewById(R.id.join_family_button);
        createFamilyButton = view.findViewById(R.id.create_family_button);
        currentFamilyCode = view.findViewById(R.id.current_family_code);

        nameSourceSpinner = view.findViewById(R.id.name_source_spinner);
        customNameInput = view.findViewById(R.id.custom_name_input);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        user = FirebaseAuth.getInstance().getCurrentUser();
        prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);

        // Показываем имя и почту
        if (account != null) {
            nameTextView.setText(account.getDisplayName());
            emailTextView.setText(account.getEmail());
        }

        // Настройка выпадающего списка
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.name_source_options,
                R.layout.spinner_item_black
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nameSourceSpinner.setAdapter(adapter);

        // Восстановление выбора
        int savedIndex = prefs.getInt("name_source_index", 0);
        String savedCustomName = prefs.getString("custom_name", "");
        nameSourceSpinner.setSelection(savedIndex);

        updateNameField(savedIndex, savedCustomName);

        nameSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateNameField(position, prefs.getString("custom_name", ""));
                // Автоматически сохраняем выбор
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("name_source_index", position);
                if (position == 2) {
                    editor.putString("custom_name", customNameInput.getText().toString());
                }
                editor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Автоматически сохраняем ручной ввод имени при каждом изменении
        customNameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (nameSourceSpinner.getSelectedItemPosition() == 2) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("custom_name", s.toString());
                    editor.apply();
                }
            }
        });

        signOutButton.setOnClickListener(v -> {
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(
                    getContext(),
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            );
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
            });
        });

        db = FirebaseFirestore.getInstance();

        updateCurrentFamilyCode();

        createFamilyButton.setOnClickListener(v -> createFamily());
        joinFamilyButton.setOnClickListener(v -> {
            String code = familyCodeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                joinFamily(code);
            } else {
                familyCodeInput.setError("Введите код семьи");
            }
        });

        return view;
    }

    private void updateNameField(int pos, String savedCustomName) {
        if (user == null) return;
        if (pos == 2) { // Ввод вручную
            customNameInput.setVisibility(View.VISIBLE);
            customNameInput.setText(savedCustomName);
        } else {
            customNameInput.setVisibility(View.VISIBLE);
            if (pos == 0) {
                customNameInput.setText(user.getDisplayName());
            } else if (pos == 1) {
                customNameInput.setText(user.getEmail());
            }
            customNameInput.setVisibility(View.GONE);
        }
    }

    public void updateCurrentFamilyCode() {
        if (user == null) return;
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("familyId")) {
                String code = doc.getString("familyId");
                currentFamilyCode.setText("Код семьи: " + (code != null ? code : "-"));
            } else {
                currentFamilyCode.setText("Код семьи: -");
            }
        });
    }

    public void createFamily() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        String familyCode = generateFamilyCode();
        String userId = user.getUid();

        Map<String, Object> familyData = new HashMap<>();
        familyData.put("code", familyCode);
        familyData.put("createdBy", userId);
        familyData.put("members", Arrays.asList(userId));

        db.collection("families").document(familyCode).set(familyData)
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId).update("familyId", familyCode)
                            .addOnSuccessListener(aVoid2 -> {
                                updateCurrentFamilyCode();
                                Toast.makeText(getContext(), "Семья создана! Код: " + familyCode, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка создания семьи: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void joinFamily(String familyCode) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = user.getUid();

        DocumentReference familyRef = db.collection("families").document(familyCode);
        familyRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                familyRef.update("members", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener(aVoid -> {
                            db.collection("users").document(userId).update("familyId", familyCode)
                                    .addOnSuccessListener(aVoid2 -> {
                                        updateCurrentFamilyCode();
                                        Toast.makeText(getContext(), "Вы присоединились к семье!", Toast.LENGTH_SHORT).show();
                                    });
                        });
            } else {
                familyCodeInput.setError("Семья с таким кодом не найдена");
                Toast.makeText(getContext(), "Семья с таким кодом не найдена", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Ошибка поиска семьи: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String generateFamilyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }
}