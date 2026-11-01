package dev.astra.module.impl.combat.killaura.rotation.noise.modes;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseContext;
import dev.astra.module.impl.combat.killaura.rotation.noise.NoiseMode;

public class NeuralMode extends NoiseMode {
    public NeuralMode(KillAura parent) {
        super(parent);
    }

    @Override
    public float[] compute(NoiseContext ctx) {
        float pinkY = (float) (perlin(ctx, 0.0) * 0.5f + ctx.perlin.noise(ctx.time * 0.5f, 50.0) * 0.3f + ctx.perlin.noise(ctx.time * 0.2f, 100.0) * 0.2f);
        float pinkP = (float) (perlin(ctx, 200.0) * 0.5f + ctx.perlin.noise(ctx.time * 0.5f, 250.0) * 0.3f + ctx.perlin.noise(ctx.time * 0.2f, 300.0) * 0.2f);
        float spikeY = Math.random() < 0.05f ? (float) (Math.random() - 0.5) * 4.0f : 0.0f;
        float spikeP = Math.random() < 0.05f ? (float) (Math.random() - 0.5) * 4.0f : 0.0f;
        float yawOffset = (pinkY + spikeY) * ctx.effectiveScale * ctx.fatigueFactor;
        float pitchOffset = (pinkP + spikeP) * ctx.effectiveScale * ctx.pRatio * ctx.fatigueFactor;
        return new float[]{yawOffset, pitchOffset};
    }
}