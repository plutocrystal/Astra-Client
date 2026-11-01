package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class HybridMode extends NoiseMode {
    public HybridMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        float pY = perlin(ctx, 0.0);
        float pP = perlin(ctx, 100.0);
        float wY = wave(ctx.time, ctx.freq);
        float wP = wave(ctx.time + 50.0f, ctx.freq);
        float gY = gaussian(ctx);
        float gP = gaussian(ctx);
        float nY = (float) (ctx.perlin.noise(ctx.time * 0.3f, 500.0) * 0.5f + ctx.perlin.noise(ctx.time * 0.1f, 600.0) * 0.5f);
        float nP = (float) (ctx.perlin.noise(ctx.time * 0.3f, 700.0) * 0.5f + ctx.perlin.noise(ctx.time * 0.1f, 800.0) * 0.5f);
        float sY = (float) Math.cos(ctx.time * ctx.freq * 2.0f) * 0.5f;
        float sP = (float) Math.sin(ctx.time * ctx.freq * 2.0f) * 0.5f;
        float bY = ctx.brownianYaw;
        float bP = ctx.brownianPitch;
        float yawOffset = (pY * 0.18f + wY * 0.12f + gY * 0.15f + nY * 0.2f + sY * 0.12f + bY * 0.23f) * ctx.effectiveScale * ctx.fatigueFactor;
        float pitchOffset = (pP * 0.18f + wP * 0.12f + gP * 0.15f + nP * 0.2f + sP * 0.12f + bP * 0.23f) * ctx.effectiveScale * ctx.pRatio * ctx.fatigueFactor;
        return new float[]{yawOffset, pitchOffset};
    }
}