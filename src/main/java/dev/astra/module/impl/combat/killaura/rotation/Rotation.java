package dev.astra.module.impl.combat.killaura.rotation;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.combat.killaura.rotation.noise.Noise;
import dev.astra.module.impl.combat.killaura.rotation.rotations.SimulatedAnnealingRotation;
import dev.astra.util.RandomUtils;
import dev.astra.util.rotation.RotationManager;
import dev.astra.util.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Rotation {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura parent;

    private float lastYaw;
    private float lastPitch;
    private boolean hasRotation = false;

    private final SimulatedAnnealingRotation annealing;
    private final Noise noise;

    public Rotation(KillAura parent) {
        this.parent = parent;
        this.annealing = new SimulatedAnnealingRotation(parent);
        this.noise = new Noise(parent);
    }

    private static float normalizeYaw(float yaw) {
        if (Float.isNaN(yaw) || Float.isInfinite(yaw)) yaw = 0.0F;
        yaw = yaw % 360.0F;
        if (yaw >= 180.0F) yaw -= 360.0F;
        if (yaw < -180.0F) yaw += 360.0F;
        if (Float.isNaN(yaw) || Float.isInfinite(yaw)) yaw = 0.0F;
        return MathHelper.clamp_float(yaw, -180.0F, 180.0F);
    }

    private static float clampPitch(float pitch) {
        if (Float.isNaN(pitch) || Float.isInfinite(pitch)) pitch = 0.0F;
        return MathHelper.clamp_float(pitch, -90.0F, 90.0F);
    }

    public void update(EntityLivingBase target, float partialTicks) {
        if (mc.thePlayer == null || target == null) return;
        if (!hasRotation) {
            lastYaw = normalizeYaw(mc.thePlayer.rotationYaw);
            lastPitch = clampPitch(mc.thePlayer.rotationPitch);
            hasRotation = true;
        }
        lastYaw = normalizeYaw(lastYaw);
        lastPitch = clampPitch(lastPitch);

        this.noise.update(target);

        double aimX;
        double aimZ;
        double aimY;
        if (parent.smartAim.isEnabled()) {
            AxisAlignedBB box = target.getEntityBoundingBox();
            Vec3 smartVec = RotationUtils.getSmartHitVec(box, lastYaw, lastPitch);
            aimX = smartVec.xCoord;
            aimY = smartVec.yCoord;
            aimZ = smartVec.zCoord;
        } else if (parent.bestHitVec.isEnabled()) {
            AxisAlignedBB box = target.getEntityBoundingBox();
            double dist = mc.thePlayer.getDistanceToEntity(target);
            Vec3 stableVec = RotationUtils.getStableHitVec(box, dist);
            aimX = stableVec.xCoord;
            aimY = stableVec.yCoord;
            aimZ = stableVec.zCoord;
        } else {
            aimX = target.posX;
            aimY = target.posY + target.height * 0.75D;
            aimZ = target.posZ;
        }
        if (Double.isNaN(aimX) || Double.isNaN(aimY) || Double.isNaN(aimZ)) {
            aimX = target.posX;
            aimY = target.posY + target.height * 0.75D;
            aimZ = target.posZ;
        }
        float[] rots = RotationUtils.getRotations(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ,
                aimX, aimY, aimZ);
        if (rots == null || rots.length < 2 || Float.isNaN(rots[0]) || Float.isNaN(rots[1])) {
            return;
        }

        rots[0] = normalizeYaw(rots[0]);
        rots[1] = clampPitch(rots[1]);

        this.noise.apply(rots, target);

        double yawSpeedPerTick = RandomUtils.nextDouble(
                parent.yawTurnSpeed.getMinValue(), parent.yawTurnSpeed.getMaxValue());
        double pitchSpeedPerTick = RandomUtils.nextDouble(
                parent.pitchTurnSpeed.getMinValue(), parent.pitchTurnSpeed.getMaxValue());
        float stepYaw = (float) (yawSpeedPerTick * partialTicks);
        float stepPitch = (float) (pitchSpeedPerTick * partialTicks);
        if (Float.isNaN(stepYaw)) stepYaw = 0.0F;
        if (Float.isNaN(stepPitch)) stepPitch = 0.0F;

        if (parent.randomize.is("Advanced")) {
            float[] out = annealing.compute(target, target.getEntityBoundingBox(), lastYaw, lastPitch, !parent.throughWalls.isEnabled());
            if (out == null || out.length < 2 || Float.isNaN(out[0]) || Float.isNaN(out[1])) {
                out = new float[]{rots[0], clampPitch(rots[1])};
            }
            float targetYaw = normalizeYaw(out[0]);
            float targetPitch = clampPitch(out[1]);

            float advancedYaw = normalizeYaw(RotationUtils.applyGCD(RotationUtils.turnTowards(lastYaw, targetYaw, stepYaw), lastYaw));
            float advancedPitch = clampPitch(RotationUtils.applyGCD(RotationUtils.turnTowards(lastPitch, targetPitch, stepPitch), lastPitch));
            advancedPitch = clampPitch(advancedPitch);

            lastYaw = advancedYaw;
            lastPitch = advancedPitch;
            RotationManager.setRotation(advancedYaw, advancedPitch,
                    RotationManager.parseMoveFix(parent.moveFix.getValue()),
                    RotationManager.parseRotationMode(parent.rotationMode.getValue()));
            return;
        }

        float smoothedYaw = normalizeYaw(RotationUtils.applyGCD(
                RotationUtils.turnTowards(lastYaw, rots[0], stepYaw), lastYaw));
        float targetPitch = clampPitch(rots[1]);
        float smoothedPitch = clampPitch(RotationUtils.applyGCD(
                RotationUtils.turnTowards(lastPitch, targetPitch, stepPitch), lastPitch));
        smoothedPitch = clampPitch(smoothedPitch);
        lastYaw = smoothedYaw;
        lastPitch = smoothedPitch;
        RotationManager.setRotation(smoothedYaw, smoothedPitch,
                RotationManager.parseMoveFix(parent.moveFix.getValue()),
                RotationManager.parseRotationMode(parent.rotationMode.getValue()));
    }

    public void clear() {
        hasRotation = false;
        annealing.reset();
        if (mc.thePlayer != null) {
            lastYaw = normalizeYaw(mc.thePlayer.rotationYaw);
            lastPitch = clampPitch(mc.thePlayer.rotationPitch);
        } else {
            lastYaw = 0.0F;
            lastPitch = 0.0F;
        }
        RotationManager.clearRotation();
        this.noise.clear();
    }

    public float getYaw() {
        return normalizeYaw(lastYaw);
    }

    public float getPitch() {
        return clampPitch(lastPitch);
    }
}