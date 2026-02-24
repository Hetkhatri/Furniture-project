package com.shashank.platform.furnitureecommerceappui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shashank.platform.furnitureecommerceappui.R;
import com.shashank.platform.furnitureecommerceappui.models.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private List<Product> productListFull; // for filtering
    private Context context;
    private OnProductClickListener listener;
    private Set<String> favoriteProductIds = new HashSet<>();

    // Drawable resources for fallback (when no image URL)
    private int[] fallbackImages;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onFavoriteClick(Product product, boolean isFavorite);
    }

    public ProductAdapter(Context context, OnProductClickListener listener, int[] fallbackImages) {
        this.context = context;
        this.productList = new ArrayList<>();
        this.productListFull = new ArrayList<>();
        this.listener = listener;
        this.fallbackImages = fallbackImages;
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
        Product product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.US, "₹%.2f", product.getPrice()));
        holder.productCategory.setText(product.getCategory());

        // Load image: try URL first, then fallback to local drawable
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .centerCrop()
                    .into(holder.productImage);
        } else {
            // Use fallback image based on position
            int fallbackIndex = position % fallbackImages.length;
            holder.productImage.setImageResource(fallbackImages[fallbackIndex]);
        }

        // Set favorite state
        boolean isFav = favoriteProductIds.contains(product.getId());
        holder.favoriteIcon.setImageResource(
            isFav ? R.drawable.ic_favorite_red_24dp : R.drawable.ic_favorite_white_24dp
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });

        holder.favoriteIcon.setOnClickListener(v -> {
            boolean currentlyFav = favoriteProductIds.contains(product.getId());
            if (currentlyFav) {
                favoriteProductIds.remove(product.getId());
            } else {
                favoriteProductIds.add(product.getId());
            }
            notifyItemChanged(position);
            if (listener != null) listener.onFavoriteClick(product, !currentlyFav);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setProducts(List<Product> products) {
        this.productList = new ArrayList<>(products);
        this.productListFull = new ArrayList<>(products);
        notifyDataSetChanged();
    }

    public void setFavoriteIds(Set<String> favoriteIds) {
        this.favoriteProductIds = favoriteIds;
        notifyDataSetChanged();
    }

    public void filterByCategory(String category) {
        if (category == null || category.equals("All")) {
            productList = new ArrayList<>(productListFull);
        } else {
            productList = new ArrayList<>();
            for (Product p : productListFull) {
                if (p.getCategory() != null && p.getCategory().equals(category)) {
                    productList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterBySearch(String query) {
        if (query == null || query.isEmpty()) {
            productList = new ArrayList<>(productListFull);
        } else {
            productList = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (Product p : productListFull) {
                if ((p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) ||
                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery)) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery))) {
                    productList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Sort: 0 = default, 1 = price low→high, 2 = price high→low
    public void sortByPrice(int sortMode) {
        if (sortMode == 1) {
            java.util.Collections.sort(productList, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
        } else if (sortMode == 2) {
            java.util.Collections.sort(productList, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        } else {
            productList = new ArrayList<>(productListFull);
        }
        notifyDataSetChanged();
    }

    public List<Product> getProductListFull() {
        return productListFull;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        ImageView favoriteIcon;
        TextView productName;
        TextView productPrice;
        TextView productCategory;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            favoriteIcon = itemView.findViewById(R.id.product_favorite);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productCategory = itemView.findViewById(R.id.product_category);
        }
    }
}
