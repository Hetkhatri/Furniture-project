package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2500; // 2.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide status bar for immersive splash
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_FULLSCREEN);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);
        ProgressBar progress = findViewById(R.id.splash_progress);

        // Animate logo (fade in + scale up)
        logo.animate()
            .alpha(1f)
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(800)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                logo.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(400)
                    .start();
            })
            .start();

        // Animate app name (fade in after delay)
        appName.animate()
            .alpha(1f)
            .translationY(-20f)
            .setStartDelay(500)
            .setDuration(600)
            .start();

        // Animate tagline
        tagline.animate()
            .alpha(1f)
            .translationY(-10f)
            .setStartDelay(800)
            .setDuration(600)
            .start();

        // Show progress
        progress.animate()
            .alpha(1f)
            .setStartDelay(1000)
            .setDuration(400)
            .start();

        // Navigate after splash duration
        new Handler().postDelayed(this::navigateNext, SPLASH_DURATION);
    }

    private void navigateNext() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Intent intent;

        if (currentUser != null && currentUser.isEmailVerified()) {
            // Already logged in and verified
            intent = new Intent(this, HomeActivity.class);
        } else if (currentUser != null) {
            // Logged in but not verified
            intent = new Intent(this, EmailVerificationActivity.class);
        } else {
            // Not logged in
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
