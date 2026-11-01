package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class SpiralMode extends NoiseMode {
    public SpiralMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        float spiralRadius = ctx.effectiveScale * ctx.fatigueFactor * (0.5f + 0.5f * (float) ctx.perlin.noise(ctx.time * 0.3f, 400.0));
        float spiralAngle = ctx.time * ctx.freq * 2.0f;
        float spiralPitchRad = ctx.time * ctx.freq * 1.5f;
        float yawOffset = (float) Math.cos(spiralAngle) * spiralRadius;
        float pitchOffset = (float) Math.sin(spiralPitchRad) * spiralRadius * ctx.pRatio;
        return new float[]{yawOffset, pitchOffset};
    }
}