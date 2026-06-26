package com.vkmu.datadestination;

import android.os.Bundle;
import android.widget.TextView;

import com.vkmu.datadestination.debug.DebugLogger;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.GeoIPOffline;
import com.vkmu.datadestination.parser.IpAddressUtils;
import com.vkmu.datadestination.parser.PacketHub;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class GeoIpActivity extends BaseActivity{

    private static final int REFRESH_INTERVAL_MS = 3000;
    private static final int MAX_VISIBLE_DESTINATIONS = 200;

    private volatile boolean running = true;
    private Thread refreshThread;
    private String lastRenderedText = "";
    private int lastPacketCount = -1;
    private long lastNewestTimestamp = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geoip);
        setupDrawer();
        TextView txt = findViewById(R.id.txtGeoContent);

        refreshThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(REFRESH_INTERVAL_MS);

                    List<FlowPacket> packets = PacketHub.getPackets();
                    long newestTimestamp = getNewestTimestamp(packets);
                    if (packets.size() == lastPacketCount && newestTimestamp == lastNewestTimestamp) {
                        continue;
                    }

                    lastPacketCount = packets.size();
                    lastNewestTimestamp = newestTimestamp;
                    StringBuilder builder = new StringBuilder();
                    Set<String> seen = new HashSet<>();

                    for (int i = packets.size() - 1; i >= 0; i--) {
                        if (seen.size() >= MAX_VISIBLE_DESTINATIONS) break;

                        String ip = IpAddressUtils.extractDestinationIp(packets.get(i).destinationIp);
                        if (ip == null || IpAddressUtils.isLocalOrPrivate(ip)) continue;
                        if (seen.contains(ip)) continue;

                        seen.add(ip);
                        String geo = GeoIPOffline.resolve(ip);

                        builder.append(ip)
                                .append(" → ")
                                .append(geo)
                                .append("\n");
                    }

                    builder.append("TEST 8.8.8.8 → ")
                            .append(GeoIPOffline.resolve("8.8.8.8"))
                            .append("\n\n");

                    if (packets.size() > MAX_VISIBLE_DESTINATIONS) {
                        builder.append("Showing latest ")
                                .append(MAX_VISIBLE_DESTINATIONS)
                                .append(" unique public destinations.\n");
                    }

                    String output = builder.toString();
                    if (!output.equals(lastRenderedText)) {
                        lastRenderedText = output;
                        DebugLogger.log("GeoIP refresh: packets=" + packets.size()
                                + ", shown=" + seen.size());
                        runOnUiThread(() -> {
                            if (running) txt.setText(output);
                        });
                    }

                } catch (Exception ignored) {}
            }
        }, "GeoIpRefreshThread");

        refreshThread.start();
    }

    private long getNewestTimestamp(List<FlowPacket> packets) {
        if (packets.isEmpty()) return -1;
        return packets.get(packets.size() - 1).timestamp;
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (refreshThread != null) {
            refreshThread.interrupt();
            refreshThread = null;
        }
        super.onDestroy();
    }
}
