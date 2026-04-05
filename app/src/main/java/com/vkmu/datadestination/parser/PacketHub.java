package com.vkmu.datadestination.parser;

import java.util.ArrayList;
import java.util.List;

public class PacketHub {

    private static final List<FlowPacket> packets = new ArrayList<>();
    private static final int MAX_PACKETS = 1000;

    public static synchronized void push(FlowPacket packet) {

        packets.add(packet);

        if (packets.size() > MAX_PACKETS) {
            packets.remove(0);
        }
    }

    public static synchronized List<FlowPacket> getPackets() {
        return new ArrayList<>(packets);
    }
}