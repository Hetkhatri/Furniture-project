package com.shashank.platform.furnitureecommerceappui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;
import com.shashank.platform.furnitureecommerceappui.models.Order;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView orderIdText, statusText, dateText, totalText;
    private TextView shippingName, shippingPhone, shippingAddress;
    private LinearLayout itemsContainer;
    private Button cancelButton, reorderButton, downloadInvoiceButton;
    private ImageView backButton;

    private View stepPending, stepConfirmed, stepShipped, stepDelivered;
    private View line1, line2, line3;

    private FirebaseHelper firebaseHelper;
    private Order currentOrder;
    private String orderId;

    private static final int COLOR_ACTIVE = Color.parseColor("#8793eb");
    private static final int COLOR_INACTIVE = Color.parseColor("#dddddd");
    private static final String CHANNEL_ID = "invoice_download_channel";
    private static final int PERMISSION_REQUEST_CODE = 100;

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
        
        createNotificationChannel();
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
        downloadInvoiceButton = findViewById(R.id.download_invoice_button);

        stepPending = findViewById(R.id.step_pending);
        stepConfirmed = findViewById(R.id.step_confirmed);
        stepShipped = findViewById(R.id.step_shipped);
        stepDelivered = findViewById(R.id.step_delivered);
        line1 = findViewById(R.id.line_1);
        line2 = findViewById(R.id.line_2);
        line3 = findViewById(R.id.line_3);

        downloadInvoiceButton.setOnClickListener(v -> {
            if (currentOrder != null) {
                if (checkPermission()) {
                    generatePDF();
                } else {
                    requestPermission();
                }
            } else {
                Toast.makeText(this, "Order data not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF();
            } else {
                Toast.makeText(this, "Permission denied to show notifications", Toast.LENGTH_SHORT).show();
                // We generate PDF anyway, just notification might fail on Android 13+
                generatePDF();
            }
        }
    }

    private void loadOrder(String orderId) {
        firebaseHelper.getOrdersRef().child(orderId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(OrderDetailActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        currentOrder.setId(snapshot.getKey());
                        displayOrder();
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
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_product_detail, itemsContainer, false);

            ImageView productImage = itemView.findViewById(R.id.product_image);
            TextView productName = itemView.findViewById(R.id.product_name);
            TextView productPrice = itemView.findViewById(R.id.product_price);
            RatingBar ratingBar = itemView.findViewById(R.id.product_rating);
            ImageView favoriteButton = itemView.findViewById(R.id.favorite_button);

            productName.setText(item.getProductName() + " × " + item.getQuantity());
            productPrice.setText(String.format(Locale.US, "₹%.2f", item.getProductPrice() * item.getQuantity()));

            if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
                Glide.with(this).load(item.getProductImage()).into(productImage);
            }

            favoriteButton.setOnClickListener(v -> {
                String uid = firebaseHelper.getCurrentUserId();
                if (uid == null) {
                    Toast.makeText(this, "Please log in to manage favorites", Toast.LENGTH_SHORT).show();
                    return;
                }
                firebaseHelper.addToFavorites(uid, item.getProductId());
                Toast.makeText(this, "Added to favorites!", Toast.LENGTH_SHORT).show();
            });

            itemsContainer.addView(itemView);
        }
    }

    private void generatePDF() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Title
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLACK);
        canvas.drawText("ORDER INVOICE", 200, 50, titlePaint);

        // Header Info
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        String orderIdToPrint = currentOrder.getId() != null ? currentOrder.getId() : "N/A";
        canvas.drawText("Order ID: " + orderIdToPrint, 50, 100, paint);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        canvas.drawText("Date: " + sdf.format(new Date(currentOrder.getCreatedAt())), 50, 120, paint);
        String statusToPrint = currentOrder.getStatus() != null ? currentOrder.getStatus().toUpperCase() : "PENDING";
        canvas.drawText("Status: " + statusToPrint, 50, 140, paint);

        // Customer Info
        canvas.drawText("Billed To:", 50, 180, titlePaint);
        paint.setTextSize(12);
        canvas.drawText(currentOrder.getUserName() != null ? currentOrder.getUserName() : "Guest", 50, 200, paint);
        canvas.drawText(currentOrder.getUserEmail() != null ? currentOrder.getUserEmail() : "", 50, 215, paint);
        canvas.drawText(currentOrder.getShippingAddress() != null ? currentOrder.getShippingAddress() : "", 50, 230, paint);

        // Items Table Header
        canvas.drawLine(50, 260, 545, 260, paint);
        canvas.drawText("Product", 50, 280, titlePaint);
        canvas.drawText("Qty", 350, 280, titlePaint);
        canvas.drawText("Price", 450, 280, titlePaint);
        canvas.drawLine(50, 290, 545, 290, paint);

        // Items
        int y = 310;
        if (currentOrder.getItems() != null) {
            for (CartItem item : currentOrder.getItems()) {
                if (y > 750) { // Simple page break check
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }
                canvas.drawText(item.getProductName(), 50, y, paint);
                canvas.drawText(String.valueOf(item.getQuantity()), 350, y, paint);
                canvas.drawText(String.format(Locale.US, "₹%.2f", item.getProductPrice() * item.getQuantity()), 450, y, paint);
                y += 20;
            }
        }

        // Total
        canvas.drawLine(50, y + 10, 545, y + 10, paint);
        titlePaint.setTextSize(16);
        canvas.drawText("Grand Total: ₹" + String.format(Locale.US, "%.2f", currentOrder.getTotalPrice()), 350, y + 40, titlePaint);

        // Footer
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("Thank you for shopping with us!", 230, 800, paint);

        pdfDocument.finishPage(page);

        // Save file - use public downloads directory for easier access
        String fileName = "Invoice_" + (orderIdToPrint.length() > 5 ? orderIdToPrint.substring(0, 5) : orderIdToPrint) + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            showNotification(file);
            Toast.makeText(this, "Invoice downloaded to Downloads folder", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdfDocument.close();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Invoice Downloads";
            String description = "Notifications for downloaded invoices";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle("Invoice Downloaded")
                    .setContentText("Order " + (currentOrder.getId().length() > 8 ? currentOrder.getId().substring(0, 8) : currentOrder.getId()))
                    .setSubText("Tap to view PDF")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
