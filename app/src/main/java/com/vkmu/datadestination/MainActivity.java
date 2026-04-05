package com.vkmu.datadestination;

import static com.vkmu.datadestination.parser.GeoIPOffline.ipToLong;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.vkmu.datadestination.connection.DesktopClient;
import com.vkmu.datadestination.debug.DebugLogger;
import com.vkmu.datadestination.parser.GeoIPOffline;
import com.vkmu.datadestination.parser.PacketHub;
import com.vkmu.datadestination.vpn.VpnServiceImpl;

public class MainActivity extends BaseActivity {

    private static final int VPN_REQUEST_CODE = 100;
    private volatile boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DEBUG UI SETUP
        TextView debugBox = findViewById(R.id.txtDebug);
        View debugContainer = findViewById(R.id.debugContainer);

        DebugLogger.attach(debugBox);

        boolean isDebug = getSharedPreferences("app_settings", MODE_PRIVATE)
                .getBoolean("debug_mode", true);

        if (isDebug) {
            debugContainer.setVisibility(View.VISIBLE);
        } else {
            debugContainer.setVisibility(View.GONE);
        }

        // DRAWER
        setupDrawer();

        // DESKTOP RECEIVER
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        boolean enabled = prefs.getBoolean("desktop_enabled", false);
        String ip = prefs.getString("desktop_ip", "");
        int port = prefs.getInt("desktop_port", 9000);

        if (enabled && ip != null && !ip.isEmpty()) {
            DesktopClient receiver = new DesktopClient(ip, port);
            new Thread(receiver).start();
            DebugLogger.log("DesktopClient started: " + ip + ":" + port);
        } else {
            DebugLogger.log("DesktopClient disabled or not configured");
        }

        // GEOIP INIT (background)
        new Thread(() -> GeoIPOffline.init(getApplicationContext())).start();

        // GEOIP TEST LOGS
        DebugLogger.log("TEST RANGE CHECK:");
        DebugLogger.log("0.0.0.0 → " + ipToLong("0.0.0.0"));
        DebugLogger.log("255.255.255.255 → " + ipToLong("255.255.255.255"));
        DebugLogger.log("8.8.8.8 → " + ipToLong("8.8.8.8"));
        DebugLogger.log("TEST 8.8.8.8 → " + GeoIPOffline.resolve("8.8.8.8"));

        // PACKET COUNT MONITOR (safe loop)
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(3000);
                    int size = PacketHub.getPackets().size();
                    DebugLogger.log("PacketHub size: " + size);
                } catch (Exception ignored) {}
            }
        }).start();

        // TOGGLE BUTTON
        ImageButton btn = findViewById(R.id.btnToggle);
        btn.setOnClickListener(v -> {

            boolean newState = !v.isSelected();
            v.setSelected(newState);

            if (newState) {
                prepareVpn();
                Toast.makeText(this, "VPN ACTIVE", Toast.LENGTH_SHORT).show();
            } else {
                stopVpn();
                Toast.makeText(this, "VPN INACTIVE", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        View debugContainer = findViewById(R.id.debugContainer);

        boolean isDebug = getSharedPreferences("app_settings", MODE_PRIVATE)
                .getBoolean("debug_mode", true);

        if (isDebug) {
            debugContainer.setVisibility(View.VISIBLE);
        } else {
            debugContainer.setVisibility(View.GONE);
        }
    }
    private void prepareVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            startVpn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpn();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startVpn() {
        Intent intent = new Intent(this, VpnServiceImpl.class);
        intent.setAction(VpnServiceImpl.ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopVpn() {
        Intent intent = new Intent(this, VpnServiceImpl.class);
        intent.setAction(VpnServiceImpl.ACTION_STOP);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
    }
}