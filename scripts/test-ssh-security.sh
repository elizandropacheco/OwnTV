#!/usr/bin/env bash
# Local JVM tests of the actual access policy; no device, network, or credentials.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d "${TMPDIR:-/tmp}/owntv-ssh-policy.XXXXXX")"
trap 'rm -rf "$test_dir"' EXIT
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
cat > "$test_dir/OwnSshAccessPolicyTest.java" <<'JAVA'
package com.conreo.couchytv;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class OwnSshAccessPolicyTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        for (String ip : new String[] {"10.0.0.1", "172.16.0.1", "172.31.255.254", "192.168.0.1",
                "169.254.1.1", "127.0.0.1", "::1", "fc00::1", "fd00::1", "fe80::1", "::ffff:10.0.0.1"}) {
            check(OwnSshAccessPolicy.isPrivatePeer(InetAddress.getByName(ip)), "Private peer rejected: " + ip);
        }
        for (String ip : new String[] {"8.8.8.8", "172.15.255.255", "172.32.0.1", "192.0.2.1",
                "0.0.0.0", "224.0.0.1", "::", "2001:db8::1", "ff02::1", "fec0::1", "::ffff:8.8.8.8"}) {
            check(!OwnSshAccessPolicy.isPrivatePeer(InetAddress.getByName(ip)), "Public/unsupported peer accepted: " + ip);
        }
        check(!OwnSshAccessPolicy.isPrivatePeer(null), "Null peer accepted");
        InetAddress peer = InetAddress.getByName("10.0.0.1");
        OwnSshAccessPolicy limit = new OwnSshAccessPolicy();
        check(!limit.allowAttempt(peer, 0, 0), "Empty password accepted");
        check(!limit.allowAttempt(peer, 1025, 0), "Oversized password accepted");
        check(!limit.allowAttempt(InetAddress.getByName("192.0.2.1"), 20, 0), "Public auth accepted");
        for (int n = 0; n < 6; n++) check(limit.allowAttempt(peer, 1024, n), "Early per-IP rejection");
        check(!limit.allowAttempt(peer, 20, 59_999), "Per-IP budget exceeded");
        check(limit.allowAttempt(peer, 20, 60_000), "Expired attempt was not released");
        check(!limit.allowAttempt(peer, 20, 60_000), "Sliding window released too many attempts");
        OwnSshAccessPolicy global = new OwnSshAccessPolicy();
        for (int host = 1; host <= 4; host++) {
            InetAddress address = InetAddress.getByName("10.0.1." + host);
            for (int n = 0; n < 6; n++) check(global.allowAttempt(address, 20, 0), "Early global rejection");
        }
        for (int host = 1; host < 255; host++) {
            check(!global.allowAttempt(InetAddress.getByName("10.0.2." + host), 20, 1), "Global budget bypass");
        }
        check(global.allowAttempt(peer, 20, 60_000), "Expired global window was not released");
        OwnSshAccessPolicy concurrent = new OwnSshAccessPolicy();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int n = 0; n < 100; n++) tasks.add(pool.submit(() -> {
                start.await();
                if (concurrent.allowAttempt(peer, 20, 0)) accepted.incrementAndGet();
                return null;
            }));
            start.countDown();
            for (Future<?> task : tasks) task.get(5, TimeUnit.SECONDS);
            check(accepted.get() == 6, "Concurrent calls bypassed the budget");
        } finally {
            pool.shutdownNow();
        }
        System.out.println("SSH security policy passed: peer ranges, password bounds, sliding windows, global budget, concurrent attempts.");
    }
}
JAVA
"$javac_bin" -d "$test_dir/classes" \
  "$repo_dir/app/src/main/java/com/conreo/couchytv/OwnSshAccessPolicy.java" \
  "$test_dir/OwnSshAccessPolicyTest.java"
"$java_bin" -cp "$test_dir/classes" com.conreo.couchytv.OwnSshAccessPolicyTest
