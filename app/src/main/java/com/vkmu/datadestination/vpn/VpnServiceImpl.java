package com.vkmu.datadestination.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import com.vkmu.datadestination.R;
import java.io.IOException;

public class VpnServiceImpl extends VpnService {
    private static final String CHANNEL_ID = "DataDestinationVPN";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_START = "START_VPN";
    public static final String ACTION_STOP = "STOP_VPN";

    private final Object mLock = new Object();
    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;

    // --- NATIVE SECTION ---
    static {
        // Must match the name in your CMakeLists.txt: add_library(capture SHARED ...)
        System.loadLibrary("capture");
    }

    // This method name must match the C function signature
    private native void runPacketLoop(int vpnFd);

    private native void stopPacketLoop();
    // ----------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_STICKY;
        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopVpn();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            startVpn();
            return START_STICKY;
        }
        return START_STICKY;
    }

    private void startVpn() {
        synchronized (mLock) {
            if (vpnThread != null)
                return;

            createNotificationChannel();
            Notification notification = buildNotification();
            startForeground(NOTIFICATION_ID, notification);

            try {
                Builder builder = new Builder();
                builder.setSession("DataDestination")
                        .addAddress("10.2.2.1", 30) // Standard VPN subnet
                        .addAddress("fd00:2:2::1", 64) // IPv6 local address
                        .addRoute("0.0.0.0", 0) // Route everything IPv4
                        .addRoute("::", 0) // Route everything IPv6
                        .addDnsServer("8.8.8.8") // Google DNS
                        .addDnsServer("2001:4860:4860::8888") // Google IPv6 DNS
                        .setMtu(1400); // Adjusted MTU for overhead

                vpnInterface = builder.establish();

                if (vpnInterface == null) {
                    stopSelf();
                    return;
                }

                // Pass the FD to C. This blocks the thread.
                final int fd = vpnInterface.getFd();
                vpnThread = new Thread(() -> {
                    runPacketLoop(fd);
                }, "VpnNativeThread");

                vpnThread.start();

            } catch (Exception e) {
                stopVpn();
            }
        }
    }

    public boolean protectSocket(int fd) {
        return protect(fd);
    }

    private void stopVpn() {
        synchronized (mLock) {
            stopPacketLoop(); // Signal the C loop to exit
            try {
                if (vpnThread != null) {
                    vpnThread.join(500); // Wait for native cleanup
                    vpnThread = null;
                }
                if (vpnInterface != null) {
                    vpnInterface.close();
                    vpnInterface = null;
                }
            } catch (Exception ignored) {
            }
            stopForeground(true);
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "VPN Status", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle("VPN Active")
                .setContentText("Tracking packets...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}