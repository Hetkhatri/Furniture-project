package com.shashank.platform.furnitureecommerceappui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shashank.platform.furnitureecommerceappui.OrderDetailActivity;
import com.shashank.platform.furnitureecommerceappui.R;
import com.shashank.platform.furnitureecommerceappui.models.Order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orders;
    private Context context;

    public OrderAdapter(Context context) {
        this.context = context;
        this.orders = new ArrayList<>();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        // Order ID (truncated)
        String displayId = order.getId() != null && order.getId().length() > 8
                ? order.getId().substring(0, 8).toUpperCase()
                : (order.getId() != null ? order.getId().toUpperCase() : "N/A");
        holder.orderId.setText("Order #" + displayId);

        // Status with color
        String status = order.getStatus() != null ? order.getStatus() : "pending";
        holder.status.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
        switch (status.toLowerCase()) {
            case "pending":
                holder.status.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "confirmed":
                holder.status.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "shipped":
                holder.status.setTextColor(context.getResources().getColor(android.R.color.holo_purple));
                break;
            case "delivered":
                holder.status.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "cancelled":
                holder.status.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
            default:
                holder.status.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }

        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        holder.date.setText(sdf.format(new Date(order.getCreatedAt())));

        // Items count
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        holder.itemsCount.setText(itemCount + (itemCount == 1 ? " item" : " items"));

        // Total
        holder.total.setText(String.format(Locale.US, "₹%.2f", order.getTotalPrice()));

        // Click to open Order Detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("order_id", order.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void setOrders(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, status, date, itemsCount, total;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id_text);
            status = itemView.findViewById(R.id.order_status_text);
            date = itemView.findViewById(R.id.order_date_text);
            itemsCount = itemView.findViewById(R.id.order_items_count);
            total = itemView.findViewById(R.id.order_total_text);
        }
    }
}
