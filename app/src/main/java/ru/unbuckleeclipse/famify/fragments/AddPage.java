package ru.unbuckleeclipse.famify.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.HapticFeedbackConstants;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import ru.unbuckleeclipse.famify.R;

public class AddPage extends Fragment {
    private com.google.android.material.textfield.TextInputEditText nameInput, commentInput;
    private Button addButton;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private TextInputLayout nameInputLayout, commentInputLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_page, container, false);

        nameInputLayout = view.findViewById(R.id.product_name_input_layout);
        nameInput = view.findViewById(R.id.product_name_input);
        commentInputLayout = view.findViewById(R.id.product_comment_input_layout);
        commentInput = view.findViewById(R.id.product_comment_input);
        addButton = view.findViewById(R.id.add_product_button);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Очистка ошибок при вводе
        nameInput.addTextChangedListener(new SimpleTextWatcher(() -> {
            if (nameInputLayout.isErrorEnabled()) {
                nameInputLayout.setError(null);
                nameInputLayout.setErrorEnabled(false);
            }
        }));

        addButton.setOnClickListener(v -> addProduct());

        return view;
    }

    private void addProduct() {
        String name = nameInput.getText().toString().trim();
        String comment = commentInput.getText().toString().trim();

        // Получаем имя из профиля
        SharedPreferences prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        int nameSourceIndex = prefs.getInt("name_source_index", 0);
        String addedBy;

        if (nameSourceIndex == 2) {
            addedBy = prefs.getString("custom_name", "");
        } else if (nameSourceIndex == 0) {
            addedBy = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        } else {
            addedBy = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        // Валидация полей
        if (name.isEmpty()) {
            showMaterialError(nameInputLayout, "Введите название продукта");
            return;
        }

        if (addedBy == null || addedBy.isEmpty()) {
            showMaterialError(nameInputLayout, "Заполните данные профиля");
            return;
        }

        if (user == null) return;

        db.collection("users").document(user.getUid()).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("familyId")) {
                String familyId = userDoc.getString("familyId");
                Map<String, Object> productData = new HashMap<>();
                productData.put("name", name);
                productData.put("comment", comment);
                productData.put("addedBy", addedBy);
                productData.put("inCart", false);
                productData.put("createdAt", System.currentTimeMillis());

                db.collection("families").document(familyId).collection("products")
                        .add(productData)
                        .addOnSuccessListener(docRef -> {
                            nameInput.setText("");
                            commentInput.setText("");
                            nameInput.requestFocus();
                        });
            } else {
                showMaterialError(nameInputLayout, "Вы не состоите в группе");
            }
        });
    }

    private void showMaterialError(TextInputLayout inputLayout, String errorMessage) {
        inputLayout.setError(errorMessage);
        try {
            inputLayout.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
        } catch (Exception e) {
            // Игнорируем если вибрация не поддерживается
        }
    }

    private class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable onTextChanged;

        SimpleTextWatcher(Runnable onTextChanged) {
            this.onTextChanged = onTextChanged;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(android.text.Editable s) {
            onTextChanged.run();
        }
    }
}