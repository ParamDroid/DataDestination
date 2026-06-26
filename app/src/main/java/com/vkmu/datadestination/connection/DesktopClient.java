package com.vkmu.datadestination.connection;

import com.vkmu.datadestination.debug.DebugLogger;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.IpAddressUtils;
import com.vkmu.datadestination.parser.PacketHub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DesktopClient implements Runnable {

    public interface SocketProtector {
        boolean protect(Socket socket) throws IOException;
    }

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int RETRY_DELAY_MS = 2000;

    private static DesktopClient activeClient;
    private static Thread activeThread;
    private static String activeHost;
    private static int activePort;

    private final String host;
    private final int port;
    private final SocketProtector socketProtector;
    private volatile boolean running = true;
    private volatile Socket activeSocket;

    public DesktopClient(String host, int port) {
        this(host, port, null);
    }

    public DesktopClient(String host, int port, SocketProtector socketProtector) {
        this.host = host;
        this.port = port;
        this.socketProtector = socketProtector;
    }

    public static synchronized void startOrUpdate(String host, int port) {
        startOrUpdate(host, port, null);
    }

    public static synchronized void startOrUpdate(
            String host,
            int port,
            SocketProtector socketProtector
    ) {
        String cleanHost = host == null ? "" : host.trim();

        if (cleanHost.isEmpty() || port <= 0 || port > 65535) {
            stopActive();
            DebugLogger.log("DesktopClient disabled or not configured");
            return;
        }

        if (activeThread != null
                && activeThread.isAlive()
                && cleanHost.equals(activeHost)
                && port == activePort) {
            return;
        }

        stopActive();

        activeHost = cleanHost;
        activePort = port;
        activeClient = new DesktopClient(cleanHost, port, socketProtector);
        activeThread = new Thread(activeClient, "DesktopClientThread");
        activeThread.start();
        DebugLogger.log("DesktopClient started: " + cleanHost + ":" + port);
    }

    public static synchronized void stopActive() {
        if (activeClient != null) {
            activeClient.stop();
            activeClient = null;
        }

        activeThread = null;
        activeHost = null;
        activePort = 0;
    }

    public void stop() {
        running = false;
        closeActiveSocket();
    }

    @Override
    public void run() {
        DebugLogger.log("Starting DesktopClient...");

        while (running) {
            DebugLogger.log("Connecting to " + host + ":" + port);

            try (Socket socket = new Socket()) {
                activeSocket = socket;

                if (socketProtector != null && !socketProtector.protect(socket)) {
                    DebugLogger.log("DesktopClient warning: socket was not protected");
                }

                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

                DebugLogger.log("Connected to " + host + ":" + port);

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;

                while ((line = reader.readLine()) != null) {

                    String cleanIp = IpAddressUtils.extractDestinationIp(line);

                    if (cleanIp == null || cleanIp.isEmpty()) continue;

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

                if (running) {
                    DebugLogger.log("DesktopClient error: " + e.getMessage());
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {}
            } finally {
                activeSocket = null;
            }
        }
    }

    private void closeActiveSocket() {
        Socket socket = activeSocket;
        if (socket == null) return;

        try {
            socket.close();
        } catch (IOException ignored) {
            DebugLogger.log("DesktopClient Closing Error unknown ");
        }
    }
}
