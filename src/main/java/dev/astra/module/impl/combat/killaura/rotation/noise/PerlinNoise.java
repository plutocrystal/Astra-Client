package dev.astra.module.impl.combat.killaura.rotation.noise;

import java.util.Random;

public class PerlinNoise {
    private final int[] p;
    public PerlinNoise(Random random) {
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;
        for (int i = 255; i > 0; i--) { int j = random.nextInt(i + 1); int t = perm[i]; perm[i] = perm[j]; perm[j] = t; }
        p = new int[512];
        for (int i = 0; i < 512; i++) p[i] = perm[i & 255];
    }
    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private double lerp(double t, double a, double b) { return a + t * (b - a); }
    private double grad(int hash, double x, double y) {
        int h = hash & 3; double u = h < 2 ? x : y, v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    public double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255;
        x -= Math.floor(x); y -= Math.floor(y);
        double u = fade(x), v = fade(y);
        int A = p[X] + Y, B = p[X + 1] + Y;
        return lerp(v, lerp(u, grad(p[A], x, y), grad(p[B], x - 1, y)), lerp(u, grad(p[A + 1], x, y - 1), grad(p[B + 1], x - 1, y - 1)));
    }
}