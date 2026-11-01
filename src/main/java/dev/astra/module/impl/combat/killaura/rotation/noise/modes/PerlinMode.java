package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class PerlinMode extends NoiseMode {
    public PerlinMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        float yawOffset = perlin(ctx, 0.0) * ctx.effectiveScale * ctx.fatigueFactor;
        float pitchOffset = perlin(ctx, 100.0) * ctx.effectiveScale * ctx.pRatio * ctx.fatigueFactor;
        return new float[]{yawOffset, pitchOffset};
    }
}