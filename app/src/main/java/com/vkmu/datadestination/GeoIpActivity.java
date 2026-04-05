package com.vkmu.datadestination;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vkmu.datadestination.debug.DebugLogger;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.GeoIPOffline;
import com.vkmu.datadestination.parser.PacketHub;

import java.util.HashSet;
import java.util.Set;

public class GeoIpActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geoip);
        setupDrawer();
        TextView txt = findViewById(R.id.txtGeoContent);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);

                    StringBuilder builder = new StringBuilder();
                    Set<String> seen = new HashSet<>();

                    for (FlowPacket p : PacketHub.getPackets()) {

                        String ip = p.destinationIp;
                        if (ip == null) continue;

                        // extract destination
                        if (ip.contains("->")) {
                            ip = ip.split("->")[1].trim();
                        }

                        // remove IPv4 port
                        if (ip.contains(":") && ip.contains(".")) {
                            ip = ip.substring(0, ip.indexOf(":"));
                        }

                        // remove IPv6 port
                        if (ip.contains(":") && !ip.contains(".")) {
                            int lastColon = ip.lastIndexOf(":");
                            if (lastColon > ip.indexOf(":")) {
                                ip = ip.substring(0, lastColon);
                            }
                        }

                        // skip private IPs
                        if (ip.startsWith("10.") ||
                                ip.startsWith("192.168.") ||
                                ip.startsWith("172.") ||
                                ip.equals("127.0.0.1") ||
                                ip.startsWith("fd") ||
                                ip.startsWith("fe80")) {
                            continue;
                        }

                        if (seen.contains(ip)) continue;
                        seen.add(ip);

                        String geo = GeoIPOffline.resolve(ip);

                        DebugLogger.log("CLEAN IP: " + ip + " → " + geo);

                        builder.append(ip)
                                .append(" → ")
                                .append(geo)
                                .append("\n");
                    }
                    builder.append("TEST 8.8.8.8 → ")
                            .append(GeoIPOffline.resolve("8.8.8.8"))
                            .append("\n\n");

                    runOnUiThread(() -> txt.setText(builder.toString()));

                } catch (Exception ignored) {}
            }
        }).start();
    }
}