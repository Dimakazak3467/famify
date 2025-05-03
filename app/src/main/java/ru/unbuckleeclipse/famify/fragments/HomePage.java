package ru.unbuckleeclipse.famify.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

import ru.unbuckleeclipse.famify.Product;
import ru.unbuckleeclipse.famify.ProductAdapter;
import ru.unbuckleeclipse.famify.R;

public class HomePage extends Fragment {
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private TextView emptyView;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private ListenerRegistration userListener, productsListener;
    private String familyId = null;

    private Button clearAllButton;
    private List<Product> currentProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_page, container, false);

        recyclerView = view.findViewById(R.id.products_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProductAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        clearAllButton = view.findViewById(R.id.clear_all_button);
        clearAllButton.setOnClickListener(v -> {
            if (getContext() == null || familyId == null || currentProducts.isEmpty()) return;
            new AlertDialog.Builder(getContext())
                    .setTitle("Очистить все?")
                    .setMessage("Вы уверены, что хотите удалить все товары? Это действие нельзя отменить.")
                    .setPositiveButton("Удалить все", (dialog, which) -> {
                        // Удаление всех продуктов
                        WriteBatch batch = db.batch();
                        for (Product p : currentProducts) {
                            if (p.getId() != null) {
                                batch.delete(db.collection("families").document(familyId)
                                        .collection("products").document(p.getId()));
                            }
                        }
                        batch.commit();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Слушатель для действий пользователя в списке
        adapter.setOnProductActionListener(new ProductAdapter.OnProductActionListener() {
            @Override
            public void onDeleteClicked(Product product) {
                if (getContext() == null || product == null || product.getId() == null || familyId == null) return;

                new AlertDialog.Builder(getContext())
                        .setTitle("Удалить товар")
                        .setMessage("Вы уверены, что хотите удалить этот товар?")
                        .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.collection("families").document(familyId)
                                        .collection("products").document(product.getId()).delete();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }

            @Override
            public void onCartChecked(Product product, boolean inCart) {
                if (product != null && product.getId() != null && familyId != null) {
                    db.collection("families").document(familyId)
                            .collection("products").document(product.getId())
                            .update("inCart", inCart);
                }
            }

            @Override
            public void onEditClicked(Product product) {
                if (getContext() == null || product == null || product.getId() == null || familyId == null) return;

                // Создаем диалог с двумя EditText
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_product, null);
                EditText nameEdit = dialogView.findViewById(R.id.edit_product_name);
                EditText descEdit = dialogView.findViewById(R.id.edit_product_desc);
                nameEdit.setText(product.getName());
                descEdit.setText(product.getComment());

                new AlertDialog.Builder(getContext())
                        .setTitle("Изменить продукт")
                        .setView(dialogView)
                        .setPositiveButton("Сохранить", (dialog, which) -> {
                            String newName = nameEdit.getText().toString().trim();
                            String newDesc = descEdit.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                db.collection("families").document(familyId)
                                        .collection("products").document(product.getId())
                                        .update("name", newName, "comment", newDesc);
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });

        observeFamilyProducts();

        return view;
    }

    private void observeFamilyProducts() {
        if (user == null) return;
        userListener = db.collection("users").document(user.getUid())
                .addSnapshotListener((userSnap, e) -> {
                    if (userSnap != null && userSnap.exists()) {
                        familyId = userSnap.getString("familyId");
                        if (familyId != null) {
                            if (productsListener != null) productsListener.remove();
                            productsListener = db.collection("families").document(familyId).collection("products")
                                    .addSnapshotListener((snapshots, err) -> {
                                        if (err != null) return;

                                        List<Product> productList = new ArrayList<>();
                                        // Создаем карту существующих продуктов для сохранения состояния
                                        Map<String, Product> existingProductsMap = new HashMap<>();
                                        for (Product existingProduct : currentProducts) {
                                            existingProductsMap.put(existingProduct.getId(), existingProduct);
                                        }

                                        for (DocumentSnapshot doc : snapshots) {
                                            Product product = doc.toObject(Product.class);
                                            if (product != null) {
                                                product.setId(doc.getId());
                                                // Восстанавливаем состояние меню, если продукт уже был
                                                Product existingProduct = existingProductsMap.get(product.getId());
                                                if (existingProduct != null) {
                                                    product.setMenuOpen(existingProduct.isMenuOpen());
                                                }
                                                productList.add(product);
                                            }
                                        }

                                        adapter.setProductList(productList);
                                        currentProducts = productList;
                                        clearAllButton.setVisibility(productList.isEmpty() ? View.GONE : View.VISIBLE);
                                        emptyView.setVisibility(productList.isEmpty() ? View.VISIBLE : View.GONE);
                                    });
                        } else {
                            adapter.setProductList(new ArrayList<>());
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
        if (productsListener != null) productsListener.remove();
    }
}