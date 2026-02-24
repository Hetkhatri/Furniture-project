package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;
import com.shashank.platform.furnitureecommerceappui.models.Order;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView orderIdText, statusText, dateText, totalText;
    private TextView shippingName, shippingPhone, shippingAddress;
    private LinearLayout itemsContainer;
    private Button cancelButton, reorderButton;
    private ImageView backButton;

    private View stepPending, stepConfirmed, stepShipped, stepDelivered;
    private View line1, line2, line3;

    private FirebaseHelper firebaseHelper;
    private Order currentOrder;
    private String orderId;

    private static final int COLOR_ACTIVE = Color.parseColor("#8793eb");
    private static final int COLOR_INACTIVE = Color.parseColor("#dddddd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        cancelButton.setOnClickListener(v -> cancelOrder());
        reorderButton.setOnClickListener(v -> reorderItems());

        orderId = getIntent().getStringExtra("order_id");
        if (orderId != null) {
            loadOrder(orderId);
        }
    }

    private void initViews() {
        orderIdText = findViewById(R.id.detail_order_id);
        statusText = findViewById(R.id.detail_status);
        dateText = findViewById(R.id.detail_date);
        totalText = findViewById(R.id.detail_total);
        shippingName = findViewById(R.id.detail_shipping_name);
        shippingPhone = findViewById(R.id.detail_shipping_phone);
        shippingAddress = findViewById(R.id.detail_shipping_address);
        itemsContainer = findViewById(R.id.items_container);
        cancelButton = findViewById(R.id.cancel_order_button);
        reorderButton = findViewById(R.id.reorder_button);
        backButton = findViewById(R.id.order_detail_back);

        stepPending = findViewById(R.id.step_pending);
        stepConfirmed = findViewById(R.id.step_confirmed);
        stepShipped = findViewById(R.id.step_shipped);
        stepDelivered = findViewById(R.id.step_delivered);
        line1 = findViewById(R.id.line_1);
        line2 = findViewById(R.id.line_2);
        line3 = findViewById(R.id.line_3);
    }

    private void loadOrder(String orderId) {
        firebaseHelper.getOrdersRef().child(orderId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        currentOrder.setId(snapshot.getKey());
                        displayOrder();
                    } else {
                        Toast.makeText(OrderDetailActivity.this,
                            "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(OrderDetailActivity.this,
                        "Error loading order", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void displayOrder() {
        // Order ID
        String displayId = currentOrder.getId() != null && currentOrder.getId().length() > 8
            ? currentOrder.getId().substring(0, 8).toUpperCase()
            : (currentOrder.getId() != null ? currentOrder.getId().toUpperCase() : "N/A");
        orderIdText.setText("Order #" + displayId);

        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
        dateText.setText("Placed on " + sdf.format(new Date(currentOrder.getCreatedAt())));

        // Status
        String status = currentOrder.getStatus() != null ? currentOrder.getStatus() : "pending";
        statusText.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
        setStatusColor(status);

        // Tracking progress
        updateTrackingProgress(status);

        // Total
        totalText.setText(String.format(Locale.US, "₹%.2f", currentOrder.getTotalPrice()));

        // Shipping info
        shippingName.setText(currentOrder.getShippingName() != null
            ? currentOrder.getShippingName() : currentOrder.getUserName());
        shippingPhone.setText(currentOrder.getShippingPhone() != null
            ? currentOrder.getShippingPhone() : (currentOrder.getPhone() != null ? currentOrder.getPhone() : ""));
        shippingAddress.setText(currentOrder.getShippingAddress() != null
            ? currentOrder.getShippingAddress() : "");

        // Items
        displayItems(currentOrder.getItems());

        // Show cancel button only for pending orders
        if ("pending".equalsIgnoreCase(status)) {
            cancelButton.setVisibility(View.VISIBLE);
        } else {
            cancelButton.setVisibility(View.GONE);
        }
    }

    private void setStatusColor(String status) {
        int color;
        switch (status.toLowerCase()) {
            case "pending": color = Color.parseColor("#FF9800"); break;
            case "confirmed": color = Color.parseColor("#2196F3"); break;
            case "shipped": color = Color.parseColor("#9C27B0"); break;
            case "delivered": color = Color.parseColor("#4CAF50"); break;
            case "cancelled": color = Color.parseColor("#F44336"); break;
            default: color = Color.GRAY;
        }
        statusText.setTextColor(color);
    }

    private void updateTrackingProgress(String status) {
        int step = 0;
        switch (status.toLowerCase()) {
            case "pending": step = 1; break;
            case "confirmed": step = 2; break;
            case "shipped": step = 3; break;
            case "delivered": step = 4; break;
            case "cancelled": step = 0; break;
        }

        setStepColor(stepPending, step >= 1);
        setLineColor(line1, step >= 2);
        setStepColor(stepConfirmed, step >= 2);
        setLineColor(line2, step >= 3);
        setStepColor(stepShipped, step >= 3);
        setLineColor(line3, step >= 4);
        setStepColor(stepDelivered, step >= 4);
    }

    private void setStepColor(View step, boolean active) {
        GradientDrawable bg = (GradientDrawable) step.getBackground().mutate();
        bg.setColor(active ? COLOR_ACTIVE : COLOR_INACTIVE);
        step.setBackground(bg);
    }

    private void setLineColor(View line, boolean active) {
        line.setBackgroundColor(active ? COLOR_ACTIVE : COLOR_INACTIVE);
    }

    private void displayItems(List<CartItem> items) {
        itemsContainer.removeAllViews();
        if (items == null) return;

        for (CartItem item : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            // Item name + qty
            TextView nameText = new TextView(this);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            nameText.setText(item.getProductName() + " × " + item.getQuantity());
            nameText.setTextColor(Color.parseColor("#333333"));
            nameText.setTextSize(14);

            // Item total price
            TextView priceText = new TextView(this);
            priceText.setText(String.format(Locale.US, "₹%.2f",
                item.getProductPrice() * item.getQuantity()));
            priceText.setTextColor(Color.parseColor("#333333"));
            priceText.setTextSize(14);
            priceText.setGravity(Gravity.END);

            row.addView(nameText);
            row.addView(priceText);
            itemsContainer.addView(row);
        }
    }

    private void cancelOrder() {
        new AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order?")
            .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                firebaseHelper.getOrdersRef().child(orderId)
                    .child("status").setValue("cancelled")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Order cancelled successfully",
                            Toast.LENGTH_SHORT).show();
                        // Reload to reflect changes
                        loadOrder(orderId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to cancel: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void reorderItems() {
        if (currentOrder == null || currentOrder.getItems() == null) return;

        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add all items back to cart
        for (CartItem item : currentOrder.getItems()) {
            CartItem cartItem = new CartItem(
                item.getProductId(),
                item.getProductName(),
                item.getProductImage() != null ? item.getProductImage() : "",
                item.getProductPrice(),
                item.getQuantity()
            );
            firebaseHelper.addToCart(uid, cartItem);
        }

        Toast.makeText(this, "Items added to cart!", Toast.LENGTH_SHORT).show();
        Intent cartIntent = new Intent(this, CartActivity.class);
        startActivity(cartIntent);
    }
}
