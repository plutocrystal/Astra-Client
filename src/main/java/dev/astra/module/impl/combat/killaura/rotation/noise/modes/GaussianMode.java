package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class GaussianMode extends NoiseMode {
    public GaussianMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        float yawOffset = gaussian(ctx) * ctx.effectiveScale * ctx.fatigueFactor;
        float pitchOffset = gaussian(ctx) * ctx.effectiveScale * ctx.pRatio * ctx.fatigueFactor;
        return new float[]{yawOffset, pitchOffset};
    }
}