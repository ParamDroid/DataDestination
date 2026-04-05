package com.vkmu.datadestination.utils;
import android.content.Context;
import android.util.Log;

public class SettingsManager {

    public static String getApiKey(Context context) {
        String key = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getString("abuse_api_key", "");
        Log.d("SettingsManager", "Reading API Key: " + key);
        return key;
    }

    public static boolean isDebugEnabled(Context context) {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("debug_mode", true);
    }
}