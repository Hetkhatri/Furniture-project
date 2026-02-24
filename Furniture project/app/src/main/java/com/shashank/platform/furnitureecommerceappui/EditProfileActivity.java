package com.shashank.platform.furnitureecommerceappui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText nameInput, phoneInput, emailInput;
    private EditText addressInput, cityInput, stateInput, zipInput;
    private Button saveButton;
    private ImageView backButton;
    private ProgressBar editProgress;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        saveButton.setOnClickListener(v -> saveProfile());

        loadCurrentProfile();
    }

    private void initViews() {
        nameInput = findViewById(R.id.edit_name);
        phoneInput = findViewById(R.id.edit_phone);
        emailInput = findViewById(R.id.edit_email);
        addressInput = findViewById(R.id.edit_address);
        cityInput = findViewById(R.id.edit_city);
        stateInput = findViewById(R.id.edit_state);
        zipInput = findViewById(R.id.edit_zip);
        saveButton = findViewById(R.id.edit_save_button);
        backButton = findViewById(R.id.edit_profile_back);
        editProgress = findViewById(R.id.edit_profile_progress);
    }

    private void loadCurrentProfile() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editProgress.setVisibility(View.VISIBLE);

        firebaseHelper.getUserRef(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                editProgress.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String city = snapshot.child("city").getValue(String.class);
                    String state = snapshot.child("state").getValue(String.class);
                    String zip = snapshot.child("zip").getValue(String.class);

                    if (name != null) nameInput.setText(name);
                    if (phone != null) phoneInput.setText(phone);
                    if (email != null) emailInput.setText(email);
                    if (address != null) addressInput.setText(address);
                    if (city != null) cityInput.setText(city);
                    if (state != null) stateInput.setText(state);
                    if (zip != null) zipInput.setText(zip);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                editProgress.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        // Clear errors
        nameInput.setError(null);
        phoneInput.setError(null);
        addressInput.setError(null);
        zipInput.setError(null);

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String state = stateInput.getText().toString().trim();
        String zip = zipInput.getText().toString().trim();

        // === Validations ===
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            nameInput.requestFocus();
            return;
        }
        if (name.length() < 2) {
            nameInput.setError("Name must be at least 2 characters");
            nameInput.requestFocus();
            return;
        }
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            nameInput.setError("Name can only contain letters and spaces");
            nameInput.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(phone) && !phone.matches("^\\d{10}$")) {
            phoneInput.setError("Enter a valid 10-digit phone number");
            phoneInput.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(zip) && !zip.matches("^\\d{5,6}$")) {
            zipInput.setError("Enter a valid 5 or 6 digit ZIP code");
            zipInput.requestFocus();
            return;
        }

        // === Save to Firebase ===
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        editProgress.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("city", city);
        updates.put("state", state);
        updates.put("zip", zip);

        firebaseHelper.getUserRef(uid).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                editProgress.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                editProgress.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
