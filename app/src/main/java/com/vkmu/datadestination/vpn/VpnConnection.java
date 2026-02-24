package com.vkmu.datadestination.vpn;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.vkmu.datadestination.parser.PacketBuilder;
import com.vkmu.datadestination.parser.PacketParser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VpnConnection implements Runnable {

    private static final String TAG = "VpnConnection";

    private final ParcelFileDescriptor vpnInterface;
    private final VpnService service;
    private volatile boolean running = true;

    public VpnConnection(ParcelFileDescriptor vpnInterface, VpnService service) {
        this.vpnInterface = vpnInterface;
        this.service = service;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {

        Log.d(TAG, "VpnConnection thread started");

        try (FileInputStream input =
                     new FileInputStream(vpnInterface.getFileDescriptor());
             FileOutputStream output =
                     new FileOutputStream(vpnInterface.getFileDescriptor())) {

            byte[] buffer = new byte[1600];

            while (running && !Thread.currentThread().isInterrupted()) {

                int length = input.read(buffer);

                if (length <= 0) continue;

                if (!PacketParser.isIPv4(buffer)) continue;

                if (PacketParser.isUDP(buffer)) {

                    int destPort = PacketParser.getDestinationPort(buffer);

                    if (destPort == 53) {

                        Log.d(TAG, "DNS packet detected");

                        byte[] payload =
                                PacketParser.extractUdpPayload(buffer, length);

                        byte[] response =
                                forwardDns(payload);

                        if (response != null) {

                            byte[] replyPacket =
                                    PacketBuilder.buildUdpResponse(
                                            buffer,
                                            response
                                    );

                            output.write(replyPacket);
                            output.flush();
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "VPN error", e);
        }

        Log.d(TAG, "VpnConnection thread stopped");
    }
    private byte[] forwardDns(byte[] payload) {

        try {
            DatagramSocket socket = new DatagramSocket();
            service.protect(socket);

            socket.setSoTimeout(2000);

            InetAddress dnsServer = InetAddress.getByName("8.8.8.8");

            DatagramPacket request =
                    new DatagramPacket(payload, payload.length,
                            dnsServer, 53);

            socket.send(request);

            byte[] buffer = new byte[1500];
            DatagramPacket response =
                    new DatagramPacket(buffer, buffer.length);

            socket.receive(response);
            Log.d(TAG, "DNS response length: " + response.getLength());
            socket.close();

            byte[] dnsResponse = new byte[response.getLength()];
            System.arraycopy(buffer, 0,
                    dnsResponse, 0,
                    response.getLength());

            return dnsResponse;

        } catch (Exception e) {
            Log.e(TAG, "DNS forward failed", e);
        }

        return null;
    }
}