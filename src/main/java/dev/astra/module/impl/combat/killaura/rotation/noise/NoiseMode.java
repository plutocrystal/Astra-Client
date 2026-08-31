package dev.astra.module.impl.combat.killaura.rotation.noise;

import dev.astra.module.impl.combat.killaura.KillAura;

public abstract class NoiseMode {
    protected final KillAura parent;

    protected NoiseMode(KillAura parent) {
        this.parent = parent;
    }

    protected static float gaussian(NoiseContext ctx) {
        return (float) ctx.random.nextGaussian();
    }

    protected static float wave(float time, float frequency) {
        return (float) (Math.sin(time * frequency) * 0.6f
                + Math.sin(time * frequency * 2.17f + 1.3f) * 0.25f
                + Math.sin(time * frequency * 0.73f + 2.7f) * 0.15f);
    }

    protected static float perlin(NoiseContext ctx, double y) {
        return (float) ctx.perlin.noise(ctx.time, y);
    }

    public abstract float[] compute(NoiseContext ctx);
}