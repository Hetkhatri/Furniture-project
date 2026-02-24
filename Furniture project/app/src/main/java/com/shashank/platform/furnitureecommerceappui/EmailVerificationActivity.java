package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private TextView emailText, timerText, messageText;
    private Button verifyButton, resendButton;
    private ProgressBar verificationProgress;
    private FirebaseAuth auth;
    private CountDownTimer cooldownTimer;
    private boolean canResend = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        auth = FirebaseAuth.getInstance();

        emailText = findViewById(R.id.verification_email_text);
        messageText = findViewById(R.id.verification_message);
        timerText = findViewById(R.id.verification_timer);
        verifyButton = findViewById(R.id.verify_check_button);
        resendButton = findViewById(R.id.resend_button);
        verificationProgress = findViewById(R.id.verification_progress);

        // Display the email
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            emailText.setText(email);
        } else {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                emailText.setText(user.getEmail());
            }
        }

        verifyButton.setOnClickListener(v -> checkVerification());
        resendButton.setOnClickListener(v -> resendVerification());
    }

    private void checkVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        verificationProgress.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(false);

        // Reload user to get latest verification status
        user.reload().addOnCompleteListener(task -> {
            verificationProgress.setVisibility(View.GONE);
            verifyButton.setEnabled(true);

            if (task.isSuccessful()) {
                FirebaseUser refreshedUser = auth.getCurrentUser();
                if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Email not yet verified. Please check your inbox and click the verification link.",
                        Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Could not check verification status. Try again.",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendVerification() {
        if (!canResend) {
            Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        verificationProgress.setVisibility(View.VISIBLE);
        resendButton.setEnabled(false);

        user.sendEmailVerification().addOnCompleteListener(task -> {
            verificationProgress.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                startCooldown();
            } else {
                resendButton.setEnabled(true);
                Toast.makeText(this, "Failed to send. Try again later.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCooldown() {
        canResend = false;
        resendButton.setEnabled(false);
        timerText.setVisibility(View.VISIBLE);

        cooldownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                timerText.setText("Resend available in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                canResend = true;
                resendButton.setEnabled(true);
                timerText.setVisibility(View.GONE);
            }
        }.start();
    }

    private void navigateToLogin() {
        auth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
        }
    }
}
