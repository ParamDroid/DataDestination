package com.vkmu.datadestination;

import android.os.Bundle;
import android.widget.TextView;

import com.vkmu.datadestination.parser.AbuseChecker;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.PacketHub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AbuseActivity extends BaseActivity {

    private volatile boolean running = true;

    // global tracking
    private static final Set<String> alreadyChecked = new HashSet<>();
    private static final Map<String, String> results = new LinkedHashMap<>();
    private static final Set<String> highRisk = new HashSet<>();

    private static final int MAX_PER_CYCLE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abuse);
        setupDrawer();

        TextView txt = findViewById(R.id.txtAbuse);

        new Thread(() -> {

            while (running) {
                try {
                    Thread.sleep(2000); // safer interval

                    Set<String> seen = new HashSet<>();
                    int checked = 0;

                    for (FlowPacket p : PacketHub.getPackets()) {

                        if (checked >= MAX_PER_CYCLE) break;

                        String ip = p.destinationIp;
                        if (ip == null) continue;

                        // clean IP
                        if (ip.contains("->")) {
                            ip = ip.split("->")[1].trim();
                        }

                        if (ip.contains(":") && ip.contains(".")) {
                            ip = ip.substring(0, ip.indexOf(":"));
                        }

                        // skip duplicates in same cycle
                        if (seen.contains(ip)) continue;
                        seen.add(ip);

                        // skip local + known safe
                        if (ip.startsWith("10.") ||
                                ip.startsWith("192.168") ||
                                ip.startsWith("127.") ||
                                ip.startsWith("0.") ||
                                ip.startsWith("fd") ||
                                ip.startsWith("fe80") ||
                                ip.equals("8.8.8.8") ||
                                ip.equals("8.8.4.4")) continue;

                        // skip already checked
                        if (alreadyChecked.contains(ip)) continue;
                        alreadyChecked.add(ip);

                        String result = AbuseChecker.check(getApplicationContext(), ip);

                        checked++;

                        // store result
                        results.put(ip, result);

                        // track HIGH risk
                        if ("HIGH".equals(result)) {
                            highRisk.add(ip);
                        }
                    }

                    // memory safety
                    if (alreadyChecked.size() > 20000) alreadyChecked.clear();
                    if (results.size() > 500) results.clear();

                    // build UI output
                    StringBuilder builder = new StringBuilder();

                    builder.append("=== HIGH RISK ===\n");
                    for (String ip : highRisk) {
                        builder.append("🔴 ").append(ip).append(" → HIGH\n");
                    }

                    builder.append("\n=== ALL TRAFFIC ===\n");
                    for (Map.Entry<String, String> entry : results.entrySet()) {

                        String emoji = "🟢";
                        if ("HIGH".equals(entry.getValue())) emoji = "🔴";
                        else if ("MEDIUM".equals(entry.getValue())) emoji = "🟡";

                        builder.append(emoji)
                                .append(" ")
                                .append(entry.getKey())
                                .append(" → ")
                                .append(entry.getValue())
                                .append("\n");
                    }

                    runOnUiThread(() -> txt.setText(builder.toString()));

                } catch (Exception ignored) {}
            }

        }).start();
    }

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
    }
}