package com.shashank.platform.furnitureecommerceappui.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkUtils {

    /**
     * Checks if the device has an active network connection.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    /**
     * Checks network and shows a toast if not available. Returns true if connected.
     */
    public static boolean checkAndNotify(Context context) {
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context,
                "No internet connection. Please check your network settings.",
                Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
