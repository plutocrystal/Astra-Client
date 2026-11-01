package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class BrownianMode extends NoiseMode {
    public BrownianMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        ctx.brownianYaw += (Math.random() - 0.5) * ctx.effectiveScale * 0.3f * ctx.fatigueFactor;
        ctx.brownianPitch += (Math.random() - 0.5) * ctx.effectiveScale * 0.3f * ctx.pRatio * ctx.fatigueFactor;
        ctx.brownianYaw *= 0.92f;
        ctx.brownianPitch *= 0.92f;
        float maxB = ctx.effectiveScale * 2.5f;
        ctx.brownianYaw = Math.max(-maxB, Math.min(maxB, ctx.brownianYaw));
        ctx.brownianPitch = Math.max(-maxB, Math.min(maxB, ctx.brownianPitch));
        return new float[]{ctx.brownianYaw, ctx.brownianPitch};
    }
}