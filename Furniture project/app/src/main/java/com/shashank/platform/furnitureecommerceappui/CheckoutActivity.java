package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.shashank.platform.furnitureecommerceappui.models.Address;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private EditText nameInput, phoneInput, addressInput, cityInput, zipInput;
    private EditText cardNumber, cardExpiry, cardCvv;
    private RadioGroup paymentMethodGroup;
    private RadioButton paymentCod, paymentCard;
    private LinearLayout cardDetailsLayout;
    private TextView subtotalText, shippingText, totalText;
    private Button placeOrderButton;
    private ImageView backButton;
    private ProgressBar checkoutProgress;
    private TextView selectSavedAddressButton;

    private static final int PICK_ADDRESS_REQUEST = 1001;

    private FirebaseHelper firebaseHelper;
    private double subtotal = 0;
    private static final double SHIPPING_COST = 5.00;
    private String savedName = "";
    private String savedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        firebaseHelper = FirebaseHelper.getInstance();
        subtotal = getIntent().getDoubleExtra("total_price", 0);

        initViews();
        displayPricing();
        setupPaymentToggle();
        loadUserProfile();

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        placeOrderButton.setOnClickListener(v -> validateAndPlaceOrder());

        selectSavedAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SavedAddressesActivity.class);
            intent.putExtra("pick_address", true);
            startActivityForResult(intent, PICK_ADDRESS_REQUEST);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void initViews() {
        nameInput = findViewById(R.id.checkout_name);
        phoneInput = findViewById(R.id.checkout_phone);
        addressInput = findViewById(R.id.checkout_address);
        cityInput = findViewById(R.id.checkout_city);
        zipInput = findViewById(R.id.checkout_zip);
        cardNumber = findViewById(R.id.card_number);
        cardExpiry = findViewById(R.id.card_expiry);
        cardCvv = findViewById(R.id.card_cvv);
        paymentMethodGroup = findViewById(R.id.payment_method_group);
        paymentCod = findViewById(R.id.payment_cod);
        paymentCard = findViewById(R.id.payment_card);
        cardDetailsLayout = findViewById(R.id.card_details_layout);
        subtotalText = findViewById(R.id.checkout_subtotal);
        shippingText = findViewById(R.id.checkout_shipping);
        totalText = findViewById(R.id.checkout_total);
        placeOrderButton = findViewById(R.id.checkout_place_order);
        backButton = findViewById(R.id.checkout_back_button);
        checkoutProgress = findViewById(R.id.checkout_progress);
        selectSavedAddressButton = findViewById(R.id.btn_select_saved_address);
    }

    private void setupPaymentToggle() {
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.payment_card) {
                cardDetailsLayout.setVisibility(View.VISIBLE);
            } else {
                cardDetailsLayout.setVisibility(View.GONE);
            }
        });
    }

    private void displayPricing() {
        subtotalText.setText(String.format(Locale.US, "₹%.2f", subtotal));
        shippingText.setText(String.format(Locale.US, "₹%.2f", SHIPPING_COST));
        totalText.setText(String.format(Locale.US, "₹%.2f", subtotal + SHIPPING_COST));
    }

    private void loadUserProfile() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUserRef(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String city = snapshot.child("city").getValue(String.class);
                    String zip = snapshot.child("zip").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    if (name != null) { nameInput.setText(name); savedName = name; }
                    if (phone != null) phoneInput.setText(phone);
                    if (address != null) addressInput.setText(address);
                    if (city != null) cityInput.setText(city);
                    if (zip != null) zipInput.setText(zip);
                    if (email != null) savedEmail = email;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validateAndPlaceOrder() {
        // Clear errors
        nameInput.setError(null);
        phoneInput.setError(null);
        addressInput.setError(null);
        cityInput.setError(null);
        zipInput.setError(null);

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String zip = zipInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || name.length() < 2) {
            nameInput.setError("Enter a valid name");
            nameInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone) || !phone.matches("^\\d{10}$")) {
            phoneInput.setError("Enter a valid 10-digit phone number");
            phoneInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(address) || address.length() < 5) {
            addressInput.setError("Enter a valid address");
            addressInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(city)) {
            cityInput.setError("City is required");
            cityInput.requestFocus();
            return;
        }

        boolean isCardPayment = paymentCard.isChecked();
        String paymentMethod = isCardPayment ? "card" : "cod";

        if (isCardPayment) {
            String cardNum = cardNumber.getText().toString().trim();
            String expiry = cardExpiry.getText().toString().trim();
            String cvv = cardCvv.getText().toString().trim();

            if (cardNum.length() != 16) {
                cardNumber.setError("Enter 16-digit card number");
                cardNumber.requestFocus();
                return;
            }
            if (!expiry.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
                cardExpiry.setError("Invalid expiry (MM/YY)");
                cardExpiry.requestFocus();
                return;
            }
            if (cvv.length() != 3) {
                cardCvv.setError("Invalid CVV");
                cardCvv.requestFocus();
                return;
            }
        }

        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        checkoutProgress.setVisibility(View.VISIBLE);
        placeOrderButton.setEnabled(false);

        firebaseHelper.getCartRef(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<CartItem> items = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CartItem item = snapshot.getValue(CartItem.class);
                    if (item != null) {
                        item.setProductId(snapshot.getKey());
                        items.add(item);
                    }
                }

                if (items.isEmpty()) {
                    checkoutProgress.setVisibility(View.GONE);
                    placeOrderButton.setEnabled(true);
                    Toast.makeText(CheckoutActivity.this, "Cart is empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                verifyStockAndPlaceOrder(uid, items, name, phone, address, city, zip, paymentMethod);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                checkoutProgress.setVisibility(View.GONE);
                placeOrderButton.setEnabled(true);
            }
        });
    }

    private void verifyStockAndPlaceOrder(String uid, List<CartItem> items,
            String name, String phone, String address, String city, String zip,
            String paymentMethod) {

        final int[] checkedCount = {0};
        final boolean[] stockOk = {true};
        final StringBuilder outOfStockItems = new StringBuilder();

        for (CartItem item : items) {
            firebaseHelper.getProductRef(item.getProductId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        checkedCount[0]++;
                        if (snapshot.exists()) {
                            Integer stock = snapshot.child("stock").getValue(Integer.class);
                            if (stock == null || stock < item.getQuantity()) {
                                stockOk[0] = false;
                                outOfStockItems.append("• ").append(item.getProductName()).append("\n");
                            }
                        }

                        if (checkedCount[0] == items.size()) {
                            if (!stockOk[0]) {
                                checkoutProgress.setVisibility(View.GONE);
                                placeOrderButton.setEnabled(true);
                                new AlertDialog.Builder(CheckoutActivity.this)
                                    .setTitle("Stock Unavailable")
                                    .setMessage("Items out of stock:\n\n" + outOfStockItems.toString())
                                    .setPositiveButton("OK", null).show();
                            } else {
                                placeOrderNow(uid, items, name, phone, address, city, zip, paymentMethod);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { checkedCount[0]++; }
                });
        }
    }

    private void placeOrderNow(String uid, List<CartItem> items,
            String name, String phone, String address, String city, String zip,
            String paymentMethod) {

        String fullAddress = address + ", " + city + " " + zip;

        Order order = new Order();
        order.setUserId(uid);
        order.setUserName(savedName.isEmpty() ? name : savedName);
        order.setUserEmail(savedEmail);
        order.setItems(items);
        order.setTotalPrice(subtotal + SHIPPING_COST);
        order.setShippingAddress(fullAddress);
        order.setShippingName(name);
        order.setShippingPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("pending");
        order.setCreatedAt(System.currentTimeMillis());

        firebaseHelper.saveOrder(uid, order, new FirebaseHelper.OrderCallback() {
            @Override
            public void onSuccess(String orderId) {
                firebaseHelper.getCartRef(uid).removeValue();
                for (CartItem item : items) {
                    firebaseHelper.getProductRef(item.getProductId()).child("stock")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Integer currentStock = snapshot.getValue(Integer.class);
                                if (currentStock != null) snapshot.getRef().setValue(Math.max(0, currentStock - item.getQuantity()));
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                }
                checkoutProgress.setVisibility(View.GONE);
                Intent successIntent = new Intent(CheckoutActivity.this, OrderSuccessActivity.class);
                successIntent.putExtra("order_id", orderId);
                successIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(successIntent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }

            @Override
            public void onError(String error) {
                checkoutProgress.setVisibility(View.GONE);
                placeOrderButton.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ADDRESS_REQUEST && resultCode == RESULT_OK && data != null) {
            String addressId = data.getStringExtra("address_id");
            loadSelectedAddress(addressId);
        }
    }

    private void loadSelectedAddress(String addressId) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUsersRef().child(uid).child("addresses").child(addressId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Address adr = snapshot.getValue(Address.class);
                    if (adr != null) {
                        nameInput.setText(adr.getFullName());
                        phoneInput.setText(adr.getPhone());
                        addressInput.setText(adr.getAddressLine1());
                        cityInput.setText(adr.getCity());
                        zipInput.setText(adr.getPincode());
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }
}
