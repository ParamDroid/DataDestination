package com.vkmu.datadestination.utils;
import android.content.Context;
import android.util.Log;

public class SettingsManager {
    private static final String PREFS = "app_settings";
    public static final String DEFAULT_IPV4 = "1.1.1.1";
    public static final String DEFAULT_IPV6 = "2606:4700:4700::1111";

    public static boolean isCustomDnsEnabled(Context context) {

        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("custom_dns_enabled", false);
    }

    public static String getDnsIpv4(Context context) {

        if (!isCustomDnsEnabled(context))
            return DEFAULT_IPV4;

        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("custom_dns", DEFAULT_IPV4);
    }

    public static String getDnsIpv6(Context context) {

        if (!isCustomDnsEnabled(context))
            return DEFAULT_IPV6;

        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("custom_dns_ipv6", DEFAULT_IPV6);
    }
    public static String getApiKey(Context context) {
        String key = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("abuse_api_key", "");
        Log.d("SettingsManager", "Reading API Key ");
        return key;
    }

    public static boolean isValidIpv4(String ip) {

        try {
            return java.net.InetAddress
                    .getByName(ip)
                    .getHostAddress()
                    .equals(ip)
                    && ip.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidIpv6(String ip) {

        try {
            return java.net.InetAddress
                    .getByName(ip)
                    .getHostAddress()
                    .contains(":");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDebugEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("debug_mode", true);
    }
}