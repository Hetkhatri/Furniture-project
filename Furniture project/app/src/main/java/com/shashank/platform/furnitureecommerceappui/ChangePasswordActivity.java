package com.shashank.platform.furnitureecommerceappui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentPassInput, newPassInput, confirmPassInput;
    private Button changePassButton;
    private ProgressBar progressBar;
    private ImageView backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        currentPassInput = findViewById(R.id.current_password_input);
        newPassInput = findViewById(R.id.new_password_input);
        confirmPassInput = findViewById(R.id.confirm_password_input);
        changePassButton = findViewById(R.id.change_password_button);
        progressBar = findViewById(R.id.change_password_progress);
        backButton = findViewById(R.id.change_pass_back_button);

        backButton.setOnClickListener(v -> finish());

        changePassButton.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        String currentPass = currentPassInput.getText().toString().trim();
        String newPass = newPassInput.getText().toString().trim();
        String confirmPass = confirmPassInput.getText().toString().trim();

        if (TextUtils.isEmpty(currentPass)) {
            currentPassInput.setError("Current password required");
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            newPassInput.setError("New password required");
            return;
        }
        if (newPass.length() < 6) {
            newPassInput.setError("Password should be at least 6 characters");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            confirmPassInput.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        changePassButton.setEnabled(false);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        progressBar.setVisibility(View.GONE);
                        changePassButton.setEnabled(true);
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, "Error updating password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    changePassButton.setEnabled(true);
                    Toast.makeText(ChangePasswordActivity.this, "Authentication failed. Check current password.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
