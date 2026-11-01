package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class WaveMode extends NoiseMode {
    public WaveMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        float yawOffset = wave(ctx.time, ctx.freq) * ctx.effectiveScale * ctx.fatigueFactor;
        float pitchOffset = wave(ctx.time + 50.0f, ctx.freq) * ctx.effectiveScale * ctx.pRatio * ctx.fatigueFactor;
        return new float[]{yawOffset, pitchOffset};
    }
}