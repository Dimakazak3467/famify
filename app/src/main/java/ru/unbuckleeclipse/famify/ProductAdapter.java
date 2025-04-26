package ru.unbuckleeclipse.famify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onDeleteClicked(Product product);
        void onCartChecked(Product product, boolean inCart);
    }

    public void setOnProductActionListener(OnProductActionListener listener) {
        this.listener = listener;
    }

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    public void setProductList(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product p = productList.get(position);
        holder.name.setText(p.getName());
        holder.comment.setText(p.getComment());
        holder.addedBy.setText("Добавил: " + p.getAddedBy());
        holder.checkBox.setChecked(p.isInCart());

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(p.isInCart());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null && buttonView.isPressed()) {
                listener.onCartChecked(p, isChecked);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(p);
        });
    }

    @Override
    public int getItemCount() {
        return productList == null ? 0 : productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView name, comment, addedBy;
        CheckBox checkBox;
        ImageButton deleteButton;
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_product_name);
            comment = itemView.findViewById(R.id.item_product_comment);
            addedBy = itemView.findViewById(R.id.item_product_added_by);
            checkBox = itemView.findViewById(R.id.item_product_checkbox);
            deleteButton = itemView.findViewById(R.id.item_product_delete);
        }
    }
}