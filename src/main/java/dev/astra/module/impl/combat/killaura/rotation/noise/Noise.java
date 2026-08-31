package dev.astra.module.impl.combat.killaura.rotation.noise;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.BrownianMode;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.GaussianMode;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.HybridMode;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.NeuralMode;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.PerlinMode;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.SpiralMode;
import dev.astra.module.impl.combat.killaura.rotation.noise.modes.WaveMode;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Noise {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Random random = new Random();
    private static final PerlinNoise perlinNoise = new PerlinNoise(new Random(System.nanoTime()));

    private final KillAura parent;

    private float perlinTimeAccumulator = 0.0F;
    private float noiseFatigue = 0.0f;
    private int lastTargetEntityId = -1;

    private final NoiseContext ctx = new NoiseContext(random, perlinNoise);
    private final Map<String, NoiseMode> modes;

    public Noise(KillAura parent) {
        this.parent = parent;
        this.modes = new HashMap<>();
        this.modes.put("Gaussian", new GaussianMode(parent));
        this.modes.put("Perlin", new PerlinMode(parent));
        this.modes.put("Wave", new WaveMode(parent));
        this.modes.put("Hybrid", new HybridMode(parent));
        this.modes.put("Neural", new NeuralMode(parent));
        this.modes.put("Brownian", new BrownianMode(parent));
        this.modes.put("Spiral", new SpiralMode(parent));
    }

    private void updateNoiseFatigue(boolean inCombat) {
        if (inCombat) {
            this.noiseFatigue = Math.min(1.0f, this.noiseFatigue + parent.noiseFatigueRate.getValue().floatValue());
        } else {
            this.noiseFatigue *= (1.0f - parent.noiseRecoveryRate.getValue().floatValue());
            if (this.noiseFatigue < 0.001f) this.noiseFatigue = 0.0f;
        }
    }

    public void update(EntityLivingBase target) {
        int currentTargetId = target.getEntityId();
        if (currentTargetId != this.lastTargetEntityId) {
            this.noiseFatigue *= 0.6f;
            this.ctx.brownianYaw = 0.0f;
            this.ctx.brownianPitch = 0.0f;
            this.lastTargetEntityId = currentTargetId;
        }

        this.updateNoiseFatigue(true);

        if (parent.noiseEnabled.isEnabled() && !parent.noiseMode.getValue().equalsIgnoreCase("None")) {
            this.perlinTimeAccumulator += parent.noiseFrequency.getValue().floatValue() * 0.05F + 0.02F * this.noiseFatigue;
        }
    }

    public void apply(float[] rots, EntityLivingBase target) {
        if (!parent.noiseEnabled.isEnabled()) return;
        float[] n = getOffset(target);
        float noisedYaw = rots[0] + n[0];
        float noisedPitch = rots[1] + n[1];

        if (parent.noiseClampToBox.isEnabled()) {
            Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
            float checkDist = parent.range.getValue().floatValue();
            Vec3 noisedLookVec = getLookVec(noisedPitch, noisedYaw);
            Vec3 noisedEndPos = eyePos.addVector(noisedLookVec.xCoord * checkDist, noisedLookVec.yCoord * checkDist, noisedLookVec.zCoord * checkDist);
            MovingObjectPosition noisedRay = target.getEntityBoundingBox().calculateIntercept(eyePos, noisedEndPos);

            if (noisedRay != null) {
                rots[0] = noisedYaw;
                rots[1] = noisedPitch;
            } else {
                for (float scale = 0.75f; scale > 0.05f; scale -= 0.25f) {
                    float scaledYaw = rots[0] + n[0] * scale;
                    float scaledPitch = rots[1] + n[1] * scale;
                    Vec3 scaledLookVec = getLookVec(scaledPitch, scaledYaw);
                    Vec3 scaledEndPos = eyePos.addVector(scaledLookVec.xCoord * checkDist, scaledLookVec.yCoord * checkDist, scaledLookVec.zCoord * checkDist);
                    MovingObjectPosition scaledRay = target.getEntityBoundingBox().calculateIntercept(eyePos, scaledEndPos);
                    if (scaledRay != null) {
                        rots[0] = scaledYaw;
                        rots[1] = scaledPitch;
                        break;
                    }
                }
            }
        } else {
            rots[0] = noisedYaw;
            rots[1] = noisedPitch;
        }
    }

    public void clear() {
        this.perlinTimeAccumulator = 0.0F;
        this.noiseFatigue = 0.0f;
        this.lastTargetEntityId = -1;
        this.ctx.brownianYaw = 0.0f;
        this.ctx.brownianPitch = 0.0f;
    }

    private float[] getOffset(EntityLivingBase target) {
        NoiseMode mode = modes.get(parent.noiseMode.getValue());
        if (mode == null) return new float[]{0.0f, 0.0f};
        float effectiveScale = parent.noiseBaseScale.getValue().floatValue() + parent.noiseFatigueScale.getValue().floatValue() * this.noiseFatigue;
        float pRatio = parent.noisePitchRatio.getValue().floatValue();
        float time = this.perlinTimeAccumulator;
        float freq = parent.noiseFrequency.getValue().floatValue();
        if (parent.noiseDistanceScale.isEnabled() && target != null) {
            double dist = mc.thePlayer.getDistanceToEntity(target);
            effectiveScale *= (float) Math.max(1.0, 0.6 + dist / 3.0);
        }
        float fatigueFactor = 0.3f + 0.7f * this.noiseFatigue;

        this.ctx.time = time;
        this.ctx.freq = freq;
        this.ctx.effectiveScale = effectiveScale;
        this.ctx.pRatio = pRatio;
        this.ctx.fatigueFactor = fatigueFactor;

        float[] offsets = mode.compute(this.ctx);
        float yawOffset = offsets[0], pitchOffset = offsets[1];

        if (parent.noiseMicroJitter.getValue().doubleValue() > 0.0f) {
            float jitterScale = parent.noiseMicroJitter.getValue().floatValue() * (0.5f + 0.5f * this.noiseFatigue);
            float jt = time * 4.0f;
            yawOffset += (float) perlinNoise.noise(jt, 200.0) * jitterScale;
            pitchOffset += (float) perlinNoise.noise(jt, 300.0) * jitterScale * pRatio;
        }

        yawOffset += parent.noiseYawBias.getValue().floatValue();
        pitchOffset += parent.noisePitchBias.getValue().floatValue();

        if (parent.noiseGcdQuantize.isEnabled()) {
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float gcd = f * f * f * 8.0F * 0.15F;
            if (gcd > 0.0f) {
                yawOffset = Math.round(yawOffset / gcd) * gcd;
                pitchOffset = Math.round(pitchOffset / gcd) * gcd;
            }
        }

        return new float[]{yawOffset, pitchOffset};
    }

    private Vec3 getLookVec(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }
}