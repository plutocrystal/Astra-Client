package dev.astra.util;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

public class RandomUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static int nextInt(int bound) {
        if (bound <= 0) return 0;
        return SECURE_RANDOM.nextInt(bound);
    }

    public static int nextInt(int min, int max) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        return min + SECURE_RANDOM.nextInt((max - min) + 1);
    }

    public static double nextDouble() {
        return SECURE_RANDOM.nextDouble();
    }

    public static double nextDouble(double min, double max) {
        if (max < min) {
            double temp = min;
            min = max;
            max = temp;
        }
        return min + SECURE_RANDOM.nextDouble() * (max - min);
    }

    public static long nextLong(long min, long max) {
        if (max < min) {
            long temp = min;
            min = max;
            max = temp;
        }
        long range = max - min + 1;
        if (range <= 0) return min;
        return min + (Math.abs(SECURE_RANDOM.nextLong()) % range);
    }

    public static boolean nextBoolean() {
        return SECURE_RANDOM.nextBoolean();
    }

    public static float nextFloat() {
        return SECURE_RANDOM.nextFloat();
    }

    public static <T> void shuffle(List<T> list) {
        if (list == null || list.isEmpty()) return;
        Collections.shuffle(list, SECURE_RANDOM);
    }

    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(nextInt(list.size()));
    }
}