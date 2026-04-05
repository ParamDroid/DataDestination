package com.vkmu.datadestination.parser;

public class FlowPacket {

    public final String origin;
    public final String destinationIp;
    public final int sourcePort;
    public final int destinationPort;
    public final String protocol;
    public final long timestamp;

    public FlowPacket(
            String origin,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            String protocol,
            long timestamp) {

        this.origin = origin;
        this.destinationIp = destinationIp;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.timestamp = timestamp;
    }
}