package com.vkmu.datadestination.parser;

import com.vkmu.datadestination.utils.SettingsManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AbuseChecker {

    private static final Map<String, String> cache = new HashMap<>();


    public static String check(android.content.Context context, String ip) {

        if (ip == null) return "Unknown";

        // cache hit
        if (cache.containsKey(ip)) {
            return cache.get(ip);
        }

        try {
            String apiKey = SettingsManager.getApiKey(context.getApplicationContext());
            if (apiKey == null || apiKey.isEmpty()) {
                return "No API Key";
            }

            String encodedIp = URLEncoder.encode(ip, "UTF-8");
            URL url = new URL(
                    "https://api.abuseipdb.com/api/v2/check?ipAddress=" + encodedIp
            );

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Key", apiKey);
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            String result = parseAbuseScore(response.toString());

            cache.put(ip, result);
            return result;

        } catch (Exception e) {
            return "Error";
        }
    }

    private static String parseAbuseScore(String json) {
        try {
            // very basic parsing (no lib needed)
            int index = json.indexOf("abuseConfidenceScore");
            if (index == -1) return "Unknown";

            String sub = json.substring(index);
            String number = sub.replaceAll("[^0-9]", "");

            int score = Integer.parseInt(number);

            if (score > 75) return "HIGH";
            if (score > 30) return "MEDIUM";
            return "LOW";

        } catch (Exception e) {
            return "Unknown";
        }
    }
}
