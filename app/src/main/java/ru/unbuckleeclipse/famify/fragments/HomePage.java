package ru.unbuckleeclipse.famify.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
                        Log.d("HomePage", "familyId = " + familyId);
                        if (familyId != null) {
                            if (productsListener != null) productsListener.remove();
                            productsListener = db.collection("families").document(familyId).collection("products")
                                    .addSnapshotListener((snapshots, err) -> {
                                        if (err != null) {
                                            Log.e("HomePage", "Firestore error: ", err);
                                            return;
                                        }
                                        Log.d("HomePage", "products count = " + (snapshots != null ? snapshots.size() : 0));
                                        List<Product> productList = new ArrayList<>();
                                        if (snapshots != null) {
                                            for (DocumentSnapshot doc : snapshots) {
                                                Product product = doc.toObject(Product.class);
                                                if (product != null) product.setId(doc.getId());
                                                productList.add(product);
                                                Log.d("HomePage", "product: " + (product != null ? product.getName() : "NULL"));
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