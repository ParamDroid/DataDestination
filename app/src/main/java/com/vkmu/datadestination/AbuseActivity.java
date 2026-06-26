package com.vkmu.datadestination;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.vkmu.datadestination.parser.AbuseChecker;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.IpAddressUtils;
import com.vkmu.datadestination.parser.PacketHub;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbuseActivity extends BaseActivity {

    private volatile boolean running = true;

    // global tracking
    private static final Set<String> alreadyChecked = new HashSet<>();
    private static final Map<String, String> results = new LinkedHashMap<>();
    private static final Set<String> highRisk = new HashSet<>();
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9:.])(?:\\d{1,3}(?:\\.\\d{1,3}){3}|[0-9A-Fa-f:.%]*:[0-9A-Fa-f:.%]+)(?![A-Za-z0-9:.])"
    );

    private static final int MAX_PER_CYCLE = 3;
    private String lastRenderedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abuse);
        setupDrawer();

        TextView txt = findViewById(R.id.txtAbuse);

        // Required for clickable spans
        txt.setMovementMethod(LinkMovementMethod.getInstance());

        new Thread(() -> {

            while (running) {
                try {
                    Thread.sleep(2000); // safer interval

                    Set<String> seen = new HashSet<>();
                    int checked = 0;

                    for (FlowPacket p : PacketHub.getPackets()) {

                        if (checked >= MAX_PER_CYCLE) break;

                        String ip = IpAddressUtils.extractDestinationIp(p.destinationIp);
                        if (ip == null) continue;

                        // skip duplicates in same cycle
                        if (seen.contains(ip)) continue;
                        seen.add(ip);

                        // skip local + known safe
                        if (IpAddressUtils.isLocalOrPrivate(ip) ||
                                ip.equals("8.8.8.8") ||
                                ip.equals("8.8.4.4") ||
                                ip.equals("2001:4860:4860::8888") ||
                                ip.equals("2001:4860:4860::8844")) continue;

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

                    // Build plain text (used only to detect changes)
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

                    String output = builder.toString();

                    if (!output.equals(lastRenderedText)) {
                        lastRenderedText = output;

                        runOnUiThread(() -> {
                            SpannableStringBuilder spannable =
                                    buildClickableOutput(output);
                            txt.setText(spannable);
                        });
                    }

                } catch (Exception ignored) {
                }
            }

        }).start();
    }

    /**
     * Converts every public IP address in the text into a clickable link.
     * Clicking an IP opens AbuseIPDB in the default browser.
     */
    private SpannableStringBuilder buildClickableOutput(String text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        Matcher matcher = IP_ADDRESS_PATTERN.matcher(text);

        while (matcher.find()) {
            final String ip = IpAddressUtils.normalizeIpLiteral(matcher.group());
            if (ip == null) continue;

            int start = matcher.start();
            int end = matcher.end();

            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    openIpLookup(ip);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ssb;
    }

    /**
     * Opens the selected IP address in AbuseIPDB.
     */
    private void openIpLookup(String ip) {
        String url = "https://www.abuseipdb.com/check/" + Uri.encode(ip);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
    }
}
