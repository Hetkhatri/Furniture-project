package com.shashank.platform.furnitureecommerceappui;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class FurnitureApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable offline persistence before any other Firebase calls
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
