package ru.unbuckleeclipse.famify.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import ru.unbuckleeclipse.famify.R;

public class AddPage extends Fragment {
    private EditText nameInput, commentInput;
    private Button addButton;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_page, container, false);

        nameInput = view.findViewById(R.id.product_name_input);
        commentInput = view.findViewById(R.id.product_comment_input);
        addButton = view.findViewById(R.id.add_product_button);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        addButton.setOnClickListener(v -> addProduct());

        return view;
    }

    private void addProduct() {
        String name = nameInput.getText().toString().trim();
        String comment = commentInput.getText().toString().trim();

        // Получаем имя из профиля (SharedPreferences)
        SharedPreferences prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        int nameSourceIndex = prefs.getInt("name_source_index", 0);
        String addedBy;
        if (nameSourceIndex == 2) { // Ввод вручную
            addedBy = prefs.getString("custom_name", "");
        } else if (nameSourceIndex == 0) { // Имя аккаунта
            addedBy = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        } else { // Почта
            addedBy = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        if (name.isEmpty() || addedBy == null || addedBy.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля профиля и название продукта!", Toast.LENGTH_SHORT).show();
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
                productData.put("createdAt", System.currentTimeMillis()); // <--- добавлено

                db.collection("families").document(familyId).collection("products")
                        .add(productData)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(getContext(), "Продукт добавлен!", Toast.LENGTH_SHORT).show();
                            nameInput.setText("");
                            commentInput.setText("");
                        });
            }
        });
    }
}