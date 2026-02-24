package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.adapters.CartAdapter;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemListener {

    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private ProgressBar cartProgress;
    private TextView cartEmpty, cartTotalPrice;
    private LinearLayout cartBottomBar;
    private Button checkoutButton;
    private ImageView backButton;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        firebaseHelper = FirebaseHelper.getInstance();

        cartRecyclerView = findViewById(R.id.cart_recycler_view);
        cartProgress = findViewById(R.id.cart_progress);
        cartEmpty = findViewById(R.id.cart_empty);
        cartTotalPrice = findViewById(R.id.cart_total_price);
        cartBottomBar = findViewById(R.id.cart_bottom_bar);
        checkoutButton = findViewById(R.id.cart_checkout_button);
        backButton = findViewById(R.id.cart_back_button);

        Button startShoppingButton = findViewById(R.id.empty_cart_shop_button);
        if (startShoppingButton != null) {
            startShoppingButton.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        cartAdapter = new CartAdapter(this, this);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.setAdapter(cartAdapter);

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        checkoutButton.setOnClickListener(v -> {
            if (cartAdapter.getItemCount() == 0) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra("total_price", cartAdapter.getTotalPrice());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        loadCart();
    }

    private void loadCart() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            cartEmpty.setVisibility(View.VISIBLE);
            cartEmpty.setText("Please log in to see your cart");
            cartBottomBar.setVisibility(View.GONE);
            return;
        }

        cartProgress.setVisibility(View.VISIBLE);

        firebaseHelper.getCartRef(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                cartProgress.setVisibility(View.GONE);
                List<CartItem> items = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CartItem item = snapshot.getValue(CartItem.class);
                    if (item != null) {
                        item.setProductId(snapshot.getKey());
                        items.add(item);
                    }
                }

                cartAdapter.setCartItems(items);
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cartProgress.setVisibility(View.GONE);
                Toast.makeText(CartActivity.this,
                    "Failed to load cart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (cartAdapter.getItemCount() == 0) {
            cartEmpty.setVisibility(View.VISIBLE);
            cartRecyclerView.setVisibility(View.GONE);
            cartBottomBar.setVisibility(View.GONE);
        } else {
            cartEmpty.setVisibility(View.GONE);
            cartRecyclerView.setVisibility(View.VISIBLE);
            cartBottomBar.setVisibility(View.VISIBLE);
            cartTotalPrice.setText(String.format(Locale.US, "₹%.2f",
                cartAdapter.getTotalPrice()));
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getCartRef(uid).child(item.getProductId())
                .child("quantity").setValue(newQuantity);
    }

    @Override
    public void onRemoveItem(CartItem item) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getCartRef(uid).child(item.getProductId()).removeValue();
    }
}
