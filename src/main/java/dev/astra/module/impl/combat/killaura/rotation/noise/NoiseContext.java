package dev.astra.module.impl.combat.killaura.rotation.noise;

import java.util.Random;

public class NoiseContext {
    public final Random random;
    public final PerlinNoise perlin;

    public float time;
    public float freq;
    public float effectiveScale;
    public float pRatio;
    public float fatigueFactor;

    public float brownianYaw;
    public float brownianPitch;

    public NoiseContext(Random random, PerlinNoise perlin) {
        this.random = random;
        this.perlin = perlin;
    }
}