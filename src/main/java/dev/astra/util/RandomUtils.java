package dev.astra.util;

import java.lang.management.ManagementFactory;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class RandomUtils {

    private static final long PHI = 0x9E3779B97F4A7C15L;

    private static final AtomicLong LAST_NANO = new AtomicLong(System.nanoTime());

    private static final AtomicLong CALL_COUNTER = new AtomicLong();

    private static final AtomicLong ENTROPY_POOL = new AtomicLong(initialEntropy());

    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(RandomUtils::createSecureRandom);

    private static final ThreadLocal<long[]> RESEED_STATE = ThreadLocal.withInitial(() -> new long[]{0L, 48L + ThreadLocalRandom.current().nextLong(512L)});

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    private static long fold(long h, long v) {
        return mix64(h ^ (v + PHI + (h << 6) + (h >>> 2)));
    }

    private static long initialEntropy() {
        long h = PHI;
        h = fold(h, System.nanoTime());
        h = fold(h, System.currentTimeMillis());
        h = fold(h, Thread.currentThread().getId());
        h = fold(h, System.identityHashCode(new Object()));
        h = fold(h, Runtime.getRuntime().freeMemory());
        h = fold(h, Runtime.getRuntime().totalMemory());
        h = fold(h, ThreadLocalRandom.current().nextLong());
        try {
            h = fold(h, ManagementFactory.getRuntimeMXBean().getName().hashCode());
        } catch (Throwable ignored) {
        }
        return mix64(h);
    }

    private static SecureRandom createSecureRandom() {
        SecureRandom rng = new SecureRandom();
        rng.nextLong();
        rng.setSeed(initialEntropy());
        return rng;
    }

    private static long collectEntropy() {
        long now = System.nanoTime();
        long prev = LAST_NANO.getAndSet(now);
        long counter = CALL_COUNTER.incrementAndGet();
        long probe = System.nanoTime();
        long h = ENTROPY_POOL.get();
        h = fold(h, now);
        h = fold(h, now - prev);
        h = fold(h, probe - now);
        h = fold(h, System.currentTimeMillis());
        h = fold(h, counter);
        h = fold(h, Thread.currentThread().getId());
        h = fold(h, System.identityHashCode(new Object()));
        h = fold(h, ThreadLocalRandom.current().nextLong());
        h = fold(h, Runtime.getRuntime().freeMemory());
        return ENTROPY_POOL.accumulateAndGet(h, (a, b) -> mix64(a ^ b));
    }

    private static long nextLongCore() {
        long entropy = collectEntropy();
        SecureRandom rng = SECURE_RANDOM.get();
        long out = mix64(entropy ^ rng.nextLong());
        long[] state = RESEED_STATE.get();
        if (++state[0] >= state[1]) {
            state[0] = 0L;
            state[1] = 48L + (entropy & 511L);
            long reseed = fold(out, System.nanoTime());
            reseed = fold(reseed, Runtime.getRuntime().freeMemory());
            reseed = fold(reseed, ThreadLocalRandom.current().nextLong());
            rng.setSeed(reseed);
        }
        return out;
    }

    private static int nextBits(int bits) {
        return (int) (nextLongCore() >>> (64 - bits));
    }

    public static long nextLong() {
        return nextLongCore();
    }

    public static int nextInt(int bound) {
        if (bound <= 0) return 0;
        int r = nextBits(31);
        int m = bound - 1;
        if ((bound & m) == 0) {
            return r & m;
        }
        int u = r;
        while (u - (r = u % bound) + m < 0) {
            u = nextBits(31);
        }
        return r;
    }

    public static int nextInt(int min, int max) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        long bound = (long) max - (long) min + 1L;
        if (bound > (long) Integer.MAX_VALUE) {
            while (true) {
                int r = nextBits(32);
                if (r >= min && r <= max) return r;
            }
        }
        return min + nextInt((int) bound);
    }

    public static double nextDouble() {
        return (nextLongCore() >>> 11) * 0x1.0p-53;
    }

    public static double nextDouble(double min, double max) {
        if (max < min) {
            double temp = min;
            min = max;
            max = temp;
        }
        return min + nextDouble() * (max - min);
    }

    public static long nextLong(long min, long max) {
        if (max < min) {
            long temp = min;
            min = max;
            max = temp;
        }
        long n = max - min + 1L;
        if (n <= 0L) {
            while (true) {
                long r = nextLongCore();
                if (r >= min && r <= max) return r;
            }
        }
        long m = n - 1L;
        if ((n & m) == 0L) {
            return (nextLongCore() & m) + min;
        }
        long u = nextLongCore() >>> 1;
        while (u + m - (u = u % n) < 0L) {
            u = nextLongCore() >>> 1;
        }
        return u + min;
    }

    public static boolean nextBoolean() {
        return (nextLongCore() & 1L) == 0L;
    }

    public static float nextFloat() {
        return nextBits(24) * 0x1.0p-24f;
    }

    public static double nextGaussian() {
        double u1;
        do {
            u1 = nextDouble();
        } while (u1 <= 0.0D);
        double u2 = nextDouble();
        return Math.sqrt(-2.0D * Math.log(u1)) * Math.cos(2.0D * Math.PI * u2);
    }

    public static <T> void shuffle(List<T> list) {
        if (list == null || list.isEmpty()) return;
        for (int i = list.size(); i > 1; i--) {
            Collections.swap(list, i - 1, nextInt(i));
        }
    }

    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(nextInt(list.size()));
    }
}