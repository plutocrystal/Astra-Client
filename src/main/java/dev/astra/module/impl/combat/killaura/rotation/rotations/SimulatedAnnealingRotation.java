package dev.astra.module.impl.combat.killaura.rotation.rotations;

import dev.astra.module.impl.combat.killaura.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class SimulatedAnnealingRotation {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final KillAura parent;

    private float temperature = 3.0F;
    private float saPointX = 0.0F;
    private float saPointY = 0.0F;
    private float saPointZ = 0.0F;
    private int lastTargetId = -1;
    private boolean saActive = false;

    public SimulatedAnnealingRotation(KillAura parent) {
        this.parent = parent;
    }

    public void reset() {
        temperature = 3.0F;
        saPointX = 0.0F;
        saPointY = 0.0F;
        saPointZ = 0.0F;
        lastTargetId = -1;
        saActive = false;
    }

    public float[] compute(EntityLivingBase target, AxisAlignedBB box, float fromYaw, float fromPitch, boolean checkWalls) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);

        int currentTargetId = target.getEntityId();
        boolean targetChanged = currentTargetId != lastTargetId;

        if (!saActive || targetChanged) {
            temperature = parent.saInitTemp.getValue().floatValue();
            saPointX = (float) ((box.minX + box.maxX) / 2.0);
            saPointY = (float) ((box.minY + box.maxY) / 2.0);
            saPointZ = (float) ((box.minZ + box.maxZ) / 2.0);
            saActive = true;
            lastTargetId = currentTargetId;
        }

        double cpX = Math.max(box.minX, Math.min(box.maxX, saPointX));
        double cpY = Math.max(box.minY, Math.min(box.maxY, saPointY));
        double cpZ = Math.max(box.minZ, Math.min(box.maxZ, saPointZ));

        double currentEnergy = calculateBoxEnergy(cpX, cpY, cpZ, eyePos, fromYaw, fromPitch);

        double boxW = box.maxX - box.minX;
        double boxH = box.maxY - box.minY;
        double boxD = box.maxZ - box.minZ;
        double maxBoxDim = Math.max(boxW, Math.max(boxH, boxD));

        double perturbScale = Math.min(maxBoxDim * 0.15, temperature * 0.05 + 0.02);

        int iterations = parent.saIterations.getValue().intValue();

        for (int i = 0; i < iterations; i++) {
            double dx = rand.nextGaussian() * perturbScale;
            double dy = rand.nextGaussian() * perturbScale;
            double dz = rand.nextGaussian() * perturbScale;

            double newX = Math.max(box.minX, Math.min(box.maxX, cpX + dx));
            double newY = Math.max(box.minY, Math.min(box.maxY, cpY + dy));
            double newZ = Math.max(box.minZ, Math.min(box.maxZ, cpZ + dz));

            if (checkWalls) {
                Vec3 newVec = new Vec3(newX, newY, newZ);
                MovingObjectPosition wallHit = mc.theWorld.rayTraceBlocks(eyePos, newVec);
                if (wallHit != null) {
                    continue;
                }
            }

            double newEnergy = calculateBoxEnergy(newX, newY, newZ, eyePos, fromYaw, fromPitch);
            double deltaE = newEnergy - currentEnergy;

            double displacement = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double penalty = displacement * 0.8;
            deltaE += penalty;

            if (deltaE <= 0 || rand.nextDouble() < Math.exp(-deltaE / Math.max(0.001, temperature))) {
                cpX = newX;
                cpY = newY;
                cpZ = newZ;
                currentEnergy = newEnergy;
            }
        }

        saPointX = (float) cpX;
        saPointY = (float) cpY;
        saPointZ = (float) cpZ;

        temperature = Math.max(parent.saMinTemp.getValue().floatValue(), temperature * parent.saCoolingRate.getValue().floatValue());

        double deltaX = cpX - eyePos.xCoord;
        double deltaY = cpY - eyePos.yCoord;
        double deltaZ = cpZ - eyePos.zCoord;
        double hDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) (-Math.atan2(deltaY, hDist) * 180.0 / Math.PI);

        return new float[]{targetYaw, targetPitch};
    }

    private double calculateBoxEnergy(double px, double py, double pz, Vec3 eyePos, float playerYaw, float playerPitch) {
        double deltaX = px - eyePos.xCoord;
        double deltaY = py - eyePos.yCoord;
        double deltaZ = pz - eyePos.zCoord;
        double hDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double rotYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        double rotPitch = -Math.toDegrees(Math.atan2(deltaY, hDist));

        double yawDiff = MathHelper.wrapAngleTo180_float((float) (rotYaw - playerYaw));
        double pitchDiff = MathHelper.wrapAngleTo180_float((float) (rotPitch - playerPitch));
        double angleDiff = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double heightDiff = Math.abs(py - eyePos.yCoord);

        return 0.5 * angleDiff + 0.15 * distance + 0.35 * heightDiff;
    }
}