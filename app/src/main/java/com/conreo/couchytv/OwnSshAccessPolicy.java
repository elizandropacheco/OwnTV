package com.conreo.couchytv;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small bounded guard before password hashing. This is not a network firewall. */
final class OwnSshAccessPolicy {
    private static final long WINDOW_MS = 60_000;
    private static final int PER_ADDRESS_LIMIT = 6;
    private static final int GLOBAL_LIMIT = 24;
    private static final int ADDRESS_LIMIT = 128;

    private final ArrayDeque<Long> global = new ArrayDeque<>();
    private final Map<InetAddress, ArrayDeque<Long>> attempts = new LinkedHashMap<>();

    static boolean isPrivatePeer(InetAddress address) {
        if (address == null) return false;
        if (address.isLoopbackAddress()) return true;
        byte[] raw = address.getAddress();
        int first = raw[0] & 255;
        int second = raw[1] & 255;
        if (raw.length == 4) {
            return first == 10
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 169 && second == 254);
        }
        if (raw.length == 16) {
            return (first & 254) == 252 // IPv6 unique-local, fc00::/7.
                    || (first == 254 && (second & 192) == 128); // Link-local, fe80::/10.
        }
        return false;
    }

    /** nowMs must come from a monotonic clock; only timestamps, never passwords, are retained. */
    synchronized boolean allowAttempt(InetAddress address, int passwordLength, long nowMs) {
        if (!isPrivatePeer(address) || passwordLength < 1 || passwordLength > 1024) return false;
        expire(global, nowMs);
        Iterator<Map.Entry<InetAddress, ArrayDeque<Long>>> iterator =
                attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Long> entry = iterator.next().getValue();
            expire(entry, nowMs);
            if (entry.isEmpty()) iterator.remove();
        }
        if (global.size() >= GLOBAL_LIMIT) return false;
        ArrayDeque<Long> perAddress = attempts.get(address);
        if (perAddress == null) {
            if (attempts.size() >= ADDRESS_LIMIT) return false;
            perAddress = new ArrayDeque<>();
            attempts.put(address, perAddress);
        }
        if (perAddress.size() >= PER_ADDRESS_LIMIT) return false;
        perAddress.addLast(nowMs);
        global.addLast(nowMs);
        return true;
    }

    private static void expire(ArrayDeque<Long> entries, long nowMs) {
        while (!entries.isEmpty() && nowMs - entries.peekFirst() >= WINDOW_MS)
            entries.removeFirst();
    }
}
