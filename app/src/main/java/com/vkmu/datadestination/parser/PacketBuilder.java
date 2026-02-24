package com.vkmu.datadestination.parser;

public class PacketBuilder {

    public static byte[] buildUdpResponse(
            byte[] originalPacket,
            byte[] dnsPayload) {

        int ihl = (originalPacket[0] & 0x0F) * 4;

        int totalLength = ihl + 8 + dnsPayload.length;

        byte[] packet = new byte[totalLength];

        // Copy IP header
        System.arraycopy(originalPacket, 0, packet, 0, ihl);

        // Swap source and destination IP
        for (int i = 0; i < 4; i++) {
            packet[12 + i] = originalPacket[16 + i];
            packet[16 + i] = originalPacket[12 + i];
        }

        // UDP header
        int udpOffset = ihl;

        // Swap ports
        packet[udpOffset] = originalPacket[udpOffset + 2];
        packet[udpOffset + 1] = originalPacket[udpOffset + 3];
        packet[udpOffset + 2] = originalPacket[udpOffset];
        packet[udpOffset + 3] = originalPacket[udpOffset + 1];

        int udpLength = 8 + dnsPayload.length;
        packet[udpOffset + 4] = (byte) (udpLength >> 8);
        packet[udpOffset + 5] = (byte) udpLength;

        packet[udpOffset + 6] = 0;
        packet[udpOffset + 7] = 0;

        // Copy DNS payload
        System.arraycopy(dnsPayload, 0,
                packet, udpOffset + 8,
                dnsPayload.length);

        // Set total length
        packet[2] = (byte) (totalLength >> 8);
        packet[3] = (byte) totalLength;

        return packet;
    }
}