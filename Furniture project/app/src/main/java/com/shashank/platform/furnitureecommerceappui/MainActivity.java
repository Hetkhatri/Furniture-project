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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.shashank.platform.furnitureecommerceappui.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private TextView forgotPasswordLink;
    private ProgressBar loginProgress;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        auth = FirebaseAuth.getInstance();

        // Auto-login check
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                navigateToHome();
                finish();
                return;
            } else {
                // Not verified — send to verification screen
                Intent intent = new Intent(this, EmailVerificationActivity.class);
                intent.putExtra("email", currentUser.getEmail());
                startActivity(intent);
                finish();
                return;
            }
        }

        emailInput = findViewById(R.id.login_email_input);
        passwordInput = findViewById(R.id.login_password_input);
        loginButton = findViewById(R.id.login);
        registerLink = findViewById(R.id.register_link);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        loginProgress = findViewById(R.id.login_progress);

        loginButton.setOnClickListener(v -> attemptLogin());
        registerLink.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
        forgotPasswordLink.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void attemptLogin() {
        if (!NetworkUtils.checkAndNotify(this)) return;

        emailInput.setError(null);
        passwordInput.setError(null);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email required");
            emailInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password required");
            passwordInput.requestFocus();
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            navigateToHome();
                            finish();
                        } else if (user != null) {
                            // Email not verified
                            Intent intent = new Intent(this, EmailVerificationActivity.class);
                            intent.putExtra("email", user.getEmail());
                            startActivity(intent);
                        }
                    } else {
                        String errorMsg = task.getException() != null
                            ? task.getException().getMessage()
                            : "Login failed";
                        Toast.makeText(MainActivity.this, "Login failed: " + errorMsg,
                            Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        final EditText resetEmail = new EditText(this);
        resetEmail.setHint("Enter your email");
        resetEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        resetEmail.setPadding(48, 32, 48, 16);

        // Pre-fill if email is already entered
        String currentEmail = emailInput.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            resetEmail.setText(currentEmail);
        }

        new AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your registered email address. We'll send you a link to reset your password.")
            .setView(resetEmail)
            .setPositiveButton("Send Reset Link", (dialog, which) -> {
                String email = resetEmail.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendPasswordResetEmail(email);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendPasswordResetEmail(String email) {
        setLoading(true);
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    Toast.makeText(this,
                        "Password reset link sent to " + email + ". Check your inbox.",
                        Toast.LENGTH_LONG).show();
                } else {
                    String errorMsg = task.getException() != null
                        ? task.getException().getMessage()
                        : "Failed to send reset email";
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void navigateToHome() {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        loginProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        registerLink.setEnabled(!isLoading);
    }
}
