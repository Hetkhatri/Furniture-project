package com.shashank.platform.furnitureecommerceappui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shashank.platform.furnitureecommerceappui.R;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private Context context;
    private OnCartItemListener listener;

    private final int[] fallbackImages = {
        R.drawable.favorite_img_1, R.drawable.favorite_img_2,
        R.drawable.favorite_img_3, R.drawable.favorite_img_4,
        R.drawable.favorite_img_5, R.drawable.favorite_img_6
    };

    public interface OnCartItemListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    public CartAdapter(Context context, OnCartItemListener listener) {
        this.context = context;
        this.cartItems = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.name.setText(item.getProductName());
        holder.price.setText(String.format(Locale.US, "₹%.2f", item.getTotalPrice()));
        holder.quantity.setText(String.valueOf(item.getQuantity()));

        // Load image
        if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
            Glide.with(context).load(item.getProductImage()).centerCrop().into(holder.image);
        } else {
            int idx = position % fallbackImages.length;
            holder.image.setImageResource(fallbackImages[idx]);
        }

        holder.minus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                listener.onQuantityChanged(item, item.getQuantity() - 1);
            }
        });

        holder.plus.setOnClickListener(v -> {
            listener.onQuantityChanged(item, item.getQuantity() + 1);
        });

        holder.remove.setOnClickListener(v -> listener.onRemoveItem(item));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void setCartItems(List<CartItem> items) {
        this.cartItems = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image, remove;
        TextView name, price, quantity;
        Button minus, plus;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cart_item_image);
            name = itemView.findViewById(R.id.cart_item_name);
            price = itemView.findViewById(R.id.cart_item_price);
            quantity = itemView.findViewById(R.id.cart_item_quantity);
            minus = itemView.findViewById(R.id.cart_item_minus);
            plus = itemView.findViewById(R.id.cart_item_plus);
            remove = itemView.findViewById(R.id.cart_item_remove);
        }
    }
}
