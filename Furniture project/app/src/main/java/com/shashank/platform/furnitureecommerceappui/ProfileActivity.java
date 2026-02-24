package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView profileName, profileEmail, ordersCount, favoritesCount;
    private LinearLayout homeLinearLayout;
    private LinearLayout editProfileOption, ordersOption, cartOption, logoutOption, changePasswordOption, aboutOption, addressOption, helpOption, settingsOption;
    private LinearLayout ordersTab, favoritesTab;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupNavigation();
        loadUserProfile();
        loadCounts();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        ordersCount = findViewById(R.id.profile_orders_count);
        favoritesCount = findViewById(R.id.profile_favorites_count);
        homeLinearLayout = findViewById(R.id.home_linear_layout);
        editProfileOption = findViewById(R.id.profile_edit_option);
        ordersOption = findViewById(R.id.profile_orders_option);
        cartOption = findViewById(R.id.profile_cart_option);
        logoutOption = findViewById(R.id.profile_logout_option);
        changePasswordOption = findViewById(R.id.profile_change_password_option);
        aboutOption = findViewById(R.id.profile_about_option);
        addressOption = findViewById(R.id.profile_address_option);
        helpOption = findViewById(R.id.profile_help_option);
        settingsOption = findViewById(R.id.profile_settings_option);
        ordersTab = findViewById(R.id.profile_orders_tab);
        favoritesTab = findViewById(R.id.profile_favorites_tab);
    }

    private void setupNavigation() {
        homeLinearLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        editProfileOption.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        ordersOption.setOnClickListener(v -> {
            startActivity(new Intent(this, OrderHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        ordersTab.setOnClickListener(v -> {
            startActivity(new Intent(this, OrderHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        favoritesTab.setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        addressOption.setOnClickListener(v -> {
            startActivity(new Intent(this, SavedAddressesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cartOption.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        aboutOption.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        helpOption.setOnClickListener(v -> {
            Toast.makeText(this, "Help & Support coming soon!", Toast.LENGTH_SHORT).show();
        });

        settingsOption.setOnClickListener(v -> {
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
        });

        logoutOption.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUserRef(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    profileName.setText(snapshot.child("name").getValue(String.class));
                    profileEmail.setText(snapshot.child("email").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCounts() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getOrdersRef().child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ordersCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        firebaseHelper.getFavoritesRef(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoritesCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
