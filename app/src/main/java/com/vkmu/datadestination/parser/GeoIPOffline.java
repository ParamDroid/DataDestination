package com.vkmu.datadestination.parser;

import android.content.Context;
import android.os.Build;

import com.vkmu.datadestination.debug.DebugLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIPOffline {

    static class Range {
        long start;
        long end;
        String country;
    }

    static class RangeV6 {
        BigInteger start;
        BigInteger end;
        String country;
    }

    private static final List<Range> rangesV4 = new ArrayList<>();
    private static final List<RangeV6> rangesV6 = new ArrayList<>();
    private static volatile boolean loaded = false;
    private static final Object initLock = new Object();
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    public static void init(Context context) {
        if (loaded) return;

        synchronized (initLock) {
            if (loaded) return;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("dbip.csv"))
            )) {
                DebugLogger.log("Loading GeoIP CSV...");

                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        String[] parts = line.replace("\"", "").split(",");
                        if (parts.length < 3) continue;

                        String startIp = parts[0].trim();
                        String endIp = parts[1].trim();
                        String country = parts[2].trim();

                        if ("ZZ".equals(country)) continue;

                        if (startIp.contains(":")) {
                            RangeV6 range = new RangeV6();
                            range.start = ipToBigInt(startIp);
                            range.end = ipToBigInt(endIp);
                            range.country = country;
                            if (range.start != null && range.end != null) {
                                rangesV6.add(range);
                            }
                        } else {
                            Range range = new Range();
                            range.start = ipToLong(startIp);
                            range.end = ipToLong(endIp);
                            range.country = country;
                            rangesV4.add(range);
                        }
                    } catch (Exception e) {
                        DebugLogger.log("Error parsing GeoIP row");
                    }
                }

                Collections.sort(rangesV4, (a, b) -> Long.compare(a.start, b.start));
                Collections.sort(rangesV6, (a, b) -> a.start.compareTo(b.start));

                loaded = true;
                DebugLogger.log("GeoIP loaded: v4=" + rangesV4.size() + ", v6=" + rangesV6.size());
            } catch (Exception e) {
                DebugLogger.log("Failed to load GeoIP: " + e.getMessage());
                // e.printStackTrace();  //Remove comment if need logging
            }
        }
    }

    public static String resolve(String ip) {
        if (ip == null) return "Unknown";

        String cleanIp = IpAddressUtils.extractDestinationIp(ip);
        if (cleanIp == null) return "Unknown";

        if (!loaded) return "Loading...";

        // ✅ cache check
        if (cache.containsKey(cleanIp)) {
            return cache.get(cleanIp);
        }

        String result;

        if (IpAddressUtils.isIpv6(cleanIp)) {
            result = resolveV6(cleanIp);
        } else {
            result = resolveV4(cleanIp);
        }

        // ✅ store result
        cache.put(cleanIp, result);

        return result;
    }

    private static String resolveV4(String ip) {
        long target;
        try {
            target = ipToLong(ip);
        } catch (Exception e) {
            return "Unknown";
        }

        int left = 0;
        int right = rangesV4.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Range range = rangesV4.get(mid);

            if (target < range.start) {
                right = mid - 1;
            } else if (target > range.end) {
                left = mid + 1;
            } else {
                return range.country;
            }
        }

        return "Unknown";
    }

    private static String resolveV6(String ip) {
        BigInteger target = ipToBigInt(ip);
        if (target == null) return "Unknown";

        int left = 0;
        int right = rangesV6.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            RangeV6 range = rangesV6.get(mid);

            if (target.compareTo(range.start) < 0) {
                right = mid - 1;
            } else if (target.compareTo(range.end) > 0) {
                left = mid + 1;
            } else {
                return range.country;
            }
        }
        return "Unknown";
    }

    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= Integer.parseInt(parts[i]) & 0xFF;
        }
        return result;
    }

    public static BigInteger ipToBigInt(String ip) {
        try {
            String cleanIp = ip;
            if (ip.contains("%")) { // Handle scope IDs
                cleanIp = ip.split("%")[0];
            }
            java.net.InetAddress addr = java.net.InetAddress.getByName(cleanIp);
            byte[] bytes = addr.getAddress();
            return new BigInteger(1, bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
