package com.vkmu.datadestination.parser;

import java.net.InetAddress;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IpAddressUtils {

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern IPV4_WITH_PORT_PATTERN =
            Pattern.compile("^((?:\\d{1,3}\\.){3}\\d{1,3}):(\\d{1,5})$");

    private IpAddressUtils() {}

    public static String extractDestinationIp(String value) {
        if (value == null) return null;

        String ip = value.trim();
        if (ip.isEmpty()) return null;

        int arrow = ip.lastIndexOf("->");
        boolean tupleFormat = arrow >= 0;
        if (tupleFormat) {
            ip = ip.substring(arrow + 2).trim();
        }

        int whitespace = firstWhitespaceIndex(ip);
        if (whitespace > 0) {
            ip = ip.substring(0, whitespace).trim();
        }

        String bracketedIp = extractBracketedIp(ip);
        if (bracketedIp != null) {
            return normalizeIpLiteral(bracketedIp);
        }

        Matcher ipv4WithPort = IPV4_WITH_PORT_PATTERN.matcher(ip);
        if (ipv4WithPort.matches() && isValidPort(ipv4WithPort.group(2))) {
            return normalizeIpLiteral(ipv4WithPort.group(1));
        }

        if (tupleFormat) {
            String ipv6WithoutPort = stripTupleIpv6Port(ip);
            if (ipv6WithoutPort != null) {
                return normalizeIpLiteral(ipv6WithoutPort);
            }
        }

        return normalizeIpLiteral(ip);
    }

    public static String normalizeIpLiteral(String value) {
        if (value == null) return null;

        String ip = value.trim();
        if (ip.isEmpty()) return null;

        String bracketedIp = extractBracketedIp(ip);
        if (bracketedIp != null) {
            ip = bracketedIp;
        }

        int scopeIndex = ip.indexOf('%');
        if (scopeIndex >= 0) {
            ip = ip.substring(0, scopeIndex);
        }

        String mappedIpv4 = extractMappedIpv4(ip);
        if (mappedIpv4 != null) {
            return mappedIpv4;
        }

        if (isValidIpv4(ip)) {
            return ip;
        }

        if (!ip.contains(":")) {
            return null;
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.getAddress().length != 16) return null;
            return ip.toLowerCase(Locale.US);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isIpv6(String ip) {
        String cleanIp = normalizeIpLiteral(ip);
        return cleanIp != null && cleanIp.contains(":");
    }

    public static boolean isLocalOrPrivate(String ip) {
        String cleanIp = normalizeIpLiteral(ip);
        if (cleanIp == null) return true;

        try {
            InetAddress address = InetAddress.getByName(cleanIp);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                return true;
            }

            byte[] bytes = address.getAddress();
            if (bytes.length == 16) {
                int firstByte = bytes[0] & 0xFF;
                return (firstByte & 0xFE) == 0xFC;
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static String extractBracketedIp(String value) {
        if (!value.startsWith("[")) return null;

        int closeBracket = value.indexOf(']');
        if (closeBracket <= 1) return null;

        return value.substring(1, closeBracket).trim();
    }

    private static String stripTupleIpv6Port(String value) {
        int lastColon = value.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == value.length() - 1) return null;

        String possiblePort = value.substring(lastColon + 1);
        if (!isValidPort(possiblePort)) return null;

        String possibleIp = value.substring(0, lastColon);
        String normalized = normalizeIpLiteral(possibleIp);
        return normalized != null && normalized.contains(":") ? possibleIp : null;
    }

    private static String extractMappedIpv4(String value) {
        String lower = value.toLowerCase(Locale.US);
        int lastColon = lower.lastIndexOf(':');
        if (lastColon < 0) return null;

        String possibleIpv4 = lower.substring(lastColon + 1);
        if (!isValidIpv4(possibleIpv4)) return null;

        String prefix = lower.substring(0, lastColon);
        if ("::ffff".equals(prefix) || "0:0:0:0:0:ffff".equals(prefix)) {
            return possibleIpv4;
        }

        return null;
    }

    private static boolean isValidIpv4(String value) {
        Matcher matcher = IPV4_PATTERN.matcher(value);
        if (!matcher.matches()) return false;

        for (int i = 1; i <= 4; i++) {
            int octet;
            try {
                octet = Integer.parseInt(matcher.group(i));
            } catch (NumberFormatException e) {
                return false;
            }

            if (octet < 0 || octet > 255) return false;
        }

        return true;
    }

    private static boolean isValidPort(String value) {
        if (value == null || value.isEmpty() || value.length() > 5) return false;

        try {
            int port = Integer.parseInt(value);
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int firstWhitespaceIndex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }

        return -1;
    }
}
