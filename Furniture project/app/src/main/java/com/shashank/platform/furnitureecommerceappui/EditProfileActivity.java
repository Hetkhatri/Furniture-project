package com.shashank.platform.furnitureecommerceappui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText nameInput, phoneInput, emailInput;
    private EditText addressInput, cityInput, stateInput, zipInput;
    private Button saveButton;
    private ImageView backButton;
    private ProgressBar editProgress;
    private CircleImageView profileImageView;
    private TextView changePhotoText;

    private FirebaseHelper firebaseHelper;
    private Uri imageUri;
    private String profileImageUrl;

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

        profileImageView.setOnClickListener(v -> openFileChooser());
        changePhotoText.setOnClickListener(v -> openFileChooser());

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
        profileImageView = findViewById(R.id.profile_image);
        changePhotoText = findViewById(R.id.change_photo_text);
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
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
                    profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (name != null) nameInput.setText(name);
                    if (phone != null) phoneInput.setText(phone);
                    if (email != null) emailInput.setText(email);
                    if (address != null) addressInput.setText(address);
                    if (city != null) cityInput.setText(city);
                    if (state != null) stateInput.setText(state);
                    if (zip != null) zipInput.setText(zip);

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(EditProfileActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.profile_pic)
                                .into(profileImageView);
                    }
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
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String state = stateInput.getText().toString().trim();
        String zip = zipInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            return;
        }

        if (imageUri != null) {
            uploadImageAndSaveProfile(name, phone, address, city, state, zip);
        } else {
            saveToDatabase(name, phone, address, city, state, zip, profileImageUrl);
        }
    }

    private void uploadImageAndSaveProfile(String name, String phone, String address, String city, String state, String zip) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading...");
        pd.show();

        String uid = firebaseHelper.getCurrentUserId();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images").child(uid + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    pd.dismiss();
                    saveToDatabase(name, phone, address, city, state, zip, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToDatabase(String name, String phone, String address, String city, String state, String zip, String imageUrl) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        editProgress.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("city", city);
        updates.put("state", state);
        updates.put("zip", zip);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

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
