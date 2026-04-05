package com.vkmu.datadestination.connection;

import com.vkmu.datadestination.debug.DebugLogger;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.PacketHub;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class DesktopClient implements Runnable {

    private final String host;
    private final int port;

    public DesktopClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        DebugLogger.log("Starting DesktopClient...");

        while (true) {
            DebugLogger.log("Connecting to " + host + ":" + port);

            try (Socket socket = new Socket(host, port)) {

                DebugLogger.log("Connected to " + host + ":" + port);

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;

                while ((line = reader.readLine()) != null) {

                    String cleanIp = cleanIp(line);

                    if (cleanIp == null || cleanIp.isEmpty()) continue;

                    // optional strict IPv4 validation
                    if (!cleanIp.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) continue;

                    DebugLogger.log("Desktop IP: " + cleanIp);

                    FlowPacket packet = new FlowPacket(
                            "desktop",
                            cleanIp,
                            0,
                            0,
                            "TCP",
                            System.currentTimeMillis()
                    );

                    PacketHub.push(packet);
                }

            } catch (Exception e) {

                DebugLogger.log("DesktopClient error: " + e.getMessage());

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Normalize incoming data to clean IP
     */
    private String cleanIp(String input) {

        if (input == null) return null;

        String ip = input.trim();

        // extract destination if arrow exists
        if (ip.contains("->")) {
            ip = ip.split("->")[1].trim();
        }

        // remove port (IPv4 only)
        if (ip.contains(":") && ip.contains(".")) {
            ip = ip.substring(0, ip.indexOf(":"));
        }

        return ip;
    }
}