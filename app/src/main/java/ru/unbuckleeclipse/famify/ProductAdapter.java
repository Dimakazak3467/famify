package ru.unbuckleeclipse.famify;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onDeleteClicked(Product product);
        void onCartChecked(Product product, boolean inCart);
        void onEditClicked(Product product);
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
        holder.addedBy.setText("От: " + p.getAddedBy());
        holder.checkBox.setChecked(p.isInCart());

        if (p.getComment() != null && !p.getComment().isEmpty()) {
            holder.comment.setText(p.getComment());
            holder.comment.setVisibility(View.VISIBLE);
        } else {
            holder.comment.setVisibility(View.GONE);
        }

        if (p.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            holder.addedTime.setText(sdf.format(new Date(p.getCreatedAt())));
        } else {
            holder.addedTime.setText("");
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(p.isInCart());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null && buttonView.isPressed()) {
                listener.onCartChecked(p, isChecked);
            }
        });

        updateContainerState(holder, p);
        setupFabAnimation(holder, p);
        setupButtonClickListeners(holder, p);
    }

    private void updateContainerState(ProductViewHolder holder, Product p) {
        holder.isMenuOpen = p.isMenuOpen();
        if (p.isMenuOpen()) {
            holder.bottomContainer.setVisibility(View.VISIBLE);
            holder.bottomContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.editButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.editButton.setAlpha(1f);
            holder.deleteButton.setAlpha(1f);
        } else {
            holder.bottomContainer.setVisibility(View.GONE);
            holder.bottomContainer.getLayoutParams().height = 0;
            holder.editButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
            holder.editButton.setAlpha(0f);
            holder.deleteButton.setAlpha(0f);
        }
        holder.bottomContainer.requestLayout();
        holder.fabMenu.setRotation(p.isMenuOpen() ? 180f : 0f);
    }

    private void setupFabAnimation(ProductViewHolder holder, Product p) {
        holder.fabMenu.setOnClickListener(v -> {
            boolean expand = !p.isMenuOpen();
            p.setMenuOpen(expand);
            holder.isMenuOpen = expand;

            ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(
                    holder.fabMenu,
                    "rotation",
                    holder.fabMenu.getRotation(),
                    expand ? 180f : 0f
            );
            rotateAnim.setDuration(200);
            rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            rotateAnim.start();

            if (expand) {
                holder.bottomContainer.setVisibility(View.VISIBLE);
                holder.bottomContainer.getLayoutParams().height = 0;
                holder.bottomContainer.requestLayout();
            }

            holder.bottomContainer.post(() -> {
                int startHeight = holder.bottomContainer.getHeight();
                int targetHeight = 0;

                if (expand) {
                    // Делаем кнопки видимыми перед измерением
                    holder.editButton.setVisibility(View.VISIBLE);
                    holder.deleteButton.setVisibility(View.VISIBLE);

                    // Принудительный расчет размеров
                    holder.bottomContainer.measure(
                            View.MeasureSpec.makeMeasureSpec(holder.bottomContainer.getWidth(), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                    targetHeight = holder.bottomContainer.getMeasuredHeight();
                    startHeight = 0;
                } else {
                    targetHeight = 0;
                    startHeight = holder.bottomContainer.getHeight();
                }

                ValueAnimator heightAnim = ValueAnimator.ofInt(startHeight, targetHeight);
                heightAnim.addUpdateListener(animation -> {
                    holder.bottomContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                    holder.bottomContainer.requestLayout();
                });

                ValueAnimator alphaAnim = ValueAnimator.ofFloat(expand ? 0f : 1f, expand ? 1f : 0f);
                alphaAnim.addUpdateListener(animation -> {
                    float value = (float) animation.getAnimatedValue();
                    holder.editButton.setAlpha(value);
                    holder.deleteButton.setAlpha(value);
                });

                heightAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                heightAnim.setDuration(300);
                alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                alphaAnim.setDuration(125);

                // Фиксируем окончательные параметры после анимации
                heightAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (expand) {
                            holder.bottomContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            holder.bottomContainer.requestLayout();
                        } else {
                            holder.bottomContainer.setVisibility(View.GONE);
                            holder.editButton.setVisibility(View.GONE);
                            holder.deleteButton.setVisibility(View.GONE);
                        }
                    }
                });

                heightAnim.start();
                alphaAnim.start();
            });
        });
    }

    private void setupButtonClickListeners(ProductViewHolder holder, Product p) {
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(p);
        });

        holder.editButton.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(p);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView name, comment, addedBy, addedTime;
        CheckBox checkBox;
        MaterialButton deleteButton, editButton;
        FloatingActionButton fabMenu;
        LinearLayout bottomContainer;
        boolean isMenuOpen = false;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_product_name);
            comment = itemView.findViewById(R.id.item_product_comment);
            addedBy = itemView.findViewById(R.id.item_product_added_by);
            addedTime = itemView.findViewById(R.id.item_product_added_time);
            checkBox = itemView.findViewById(R.id.item_product_checkbox);
            deleteButton = itemView.findViewById(R.id.item_product_delete);
            editButton = itemView.findViewById(R.id.item_product_edit);
            fabMenu = itemView.findViewById(R.id.item_product_fab);
            bottomContainer = itemView.findViewById(R.id.item_product_bottom_container);
        }
    }
}