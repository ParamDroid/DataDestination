package com.vkmu.datadestination.parser;

public class PacketParser {

    public static boolean isIPv4(byte[] packet) {
        return ((packet[0] >> 4) & 0x0F) == 4;
    }

    public static boolean isUDP(byte[] packet) {
        return packet[9] == 17;
    }

    public static int getDestinationPort(byte[] packet) {

        int ihl = (packet[0] & 0x0F) * 4;

        int portHigh = packet[ihl + 2] & 0xFF;
        int portLow = packet[ihl + 3] & 0xFF;

        return (portHigh << 8) | portLow;
    }

    public static int getSourcePort(byte[] packet) {

        int ihl = (packet[0] & 0x0F) * 4;

        int portHigh = packet[ihl] & 0xFF;
        int portLow = packet[ihl + 1] & 0xFF;

        return (portHigh << 8) | portLow;
    }

    public static String getDestinationIp(byte[] packet) {

        return (packet[16] & 0xFF) + "." +
                (packet[17] & 0xFF) + "." +
                (packet[18] & 0xFF) + "." +
                (packet[19] & 0xFF);
    }

    public static byte[] extractUdpPayload(byte[] packet, int length) {

        int ihl = (packet[0] & 0x0F) * 4;
        int udpHeaderLength = 8;

        int payloadOffset = ihl + udpHeaderLength;
        int payloadLength = length - payloadOffset;

        byte[] payload = new byte[payloadLength];
        System.arraycopy(packet, payloadOffset, payload, 0, payloadLength);

        return payload;
    }
}