package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.shashank.platform.furnitureecommerceappui.models.User;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText phoneInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private TextView loginLink;
    private ProgressBar registerProgress;
    private FirebaseAuth auth;

    // Password pattern: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-])[A-Za-z\\d@$!%*?&#^()_+=\\-]{8,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        auth = FirebaseAuth.getInstance();

        nameInput = findViewById(R.id.register_name_input);
        phoneInput = findViewById(R.id.register_phone_input);
        emailInput = findViewById(R.id.register_email_input);
        passwordInput = findViewById(R.id.register_password_input);
        confirmPasswordInput = findViewById(R.id.register_confirm_password_input);
        registerButton = findViewById(R.id.register_button);
        loginLink = findViewById(R.id.login_link);
        registerProgress = findViewById(R.id.register_progress);

        registerButton.setOnClickListener(v -> attemptRegister());
        loginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        // Clear previous errors
        nameInput.setError(null);
        phoneInput.setError(null);
        emailInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // === Name Validation ===
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Full name is required");
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

        // === Phone Validation ===
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
            return;
        }
        if (!phone.matches("^\\d{10}$")) {
            phoneInput.setError("Enter a valid 10-digit phone number");
            phoneInput.requestFocus();
            return;
        }

        // === Email Validation ===
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        // === Password Validation ===
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordInput.setError("Min 8 chars: uppercase, lowercase, digit & special char");
            passwordInput.requestFocus();
            return;
        }

        // === Confirm Password ===
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        // All validations passed — create account
        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save user profile with name and phone
                            User user = new User(
                                firebaseUser.getUid(),
                                name,
                                email,
                                "user"
                            );
                            user.setPhone(phone);
                            FirebaseHelper.getInstance().saveUser(user);

                            // Send email verification
                            firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(verifyTask -> {
                                    setLoading(false);
                                    // Navigate to email verification screen
                                    Intent intent = new Intent(RegisterActivity.this,
                                        EmailVerificationActivity.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                    finish();
                                });
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null
                            ? task.getException().getMessage()
                            : "Registration failed";
                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        registerProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        loginLink.setEnabled(!isLoading);
    }
}
