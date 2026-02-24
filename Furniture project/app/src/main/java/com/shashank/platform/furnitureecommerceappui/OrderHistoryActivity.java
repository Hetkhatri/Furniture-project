package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.shashank.platform.furnitureecommerceappui.adapters.OrderAdapter;
import com.shashank.platform.furnitureecommerceappui.models.Order;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private OrderAdapter orderAdapter;
    private ProgressBar ordersProgress;
    private View ordersEmpty;
    private ImageView backButton;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_order_activity);

        firebaseHelper = FirebaseHelper.getInstance();

        ordersRecyclerView = findViewById(R.id.orders_recycler_view);
        ordersProgress = findViewById(R.id.orders_progress);
        ordersEmpty = findViewById(R.id.orders_empty);
        backButton = findViewById(R.id.orders_back_button);

        orderAdapter = new OrderAdapter(this);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(orderAdapter);

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        Button shopButton = findViewById(R.id.empty_orders_shop_button);
        if (shopButton != null) {
            shopButton.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            });
        }

        loadOrders();
    }

    private void loadOrders() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            ordersEmpty.setVisibility(View.VISIBLE);
            ((TextView) ordersEmpty.findViewById(R.id.empty_orders_shop_button)).setText("Please log in to see your orders");
            return;
        }

        ordersProgress.setVisibility(View.VISIBLE);

        firebaseHelper.getOrdersRef().orderByChild("userId").equalTo(uid)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ordersProgress.setVisibility(View.GONE);
                    List<Order> orders = new ArrayList<>();

                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            order.setId(orderSnapshot.getKey());
                            orders.add(order);
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(orders, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    if (orders.isEmpty()) {
                        ordersEmpty.setVisibility(View.VISIBLE);
                        ordersRecyclerView.setVisibility(View.GONE);
                    } else {
                        ordersEmpty.setVisibility(View.GONE);
                        ordersRecyclerView.setVisibility(View.VISIBLE);
                        orderAdapter.setOrders(orders);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    ordersProgress.setVisibility(View.GONE);
                    Toast.makeText(OrderHistoryActivity.this,
                        "Failed to load orders", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
