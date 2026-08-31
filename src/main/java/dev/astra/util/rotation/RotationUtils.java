package dev.astra.util.rotation;

import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import java.util.List;

public class RotationUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final float SNAP_THRESHOLD = 0.5F;

    public static float applyGCD(float target, float current) {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;
        float gcdVal = f1 * 0.15F;
        float diff = target - current;
        return current + Math.round(diff / gcdVal) * gcdVal;
    }

    public static float turnTowards(float current, float target, float maxStep) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        if (Math.abs(diff) < SNAP_THRESHOLD) {
            return MathHelper.wrapAngleTo180_float(target);
        }
        float step = Math.min(Math.abs(diff), maxStep);
        return MathHelper.wrapAngleTo180_float(current + Math.signum(diff) * step);
    }

    public static float[] getRotations(double posX, double posY, double posZ,
                                       double aimX, double aimY, double aimZ) {
        double diffX = aimX - posX;
        double diffY = aimY - posY;
        double diffZ = aimZ - posZ;
        double horizontalDist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontalDist));
        return new float[]{yaw, pitch};
    }

    public static Vec3 getLookVector(float yaw, float pitch) {
        float yawRad = yaw * 0.017453292F;
        float pitchRad = pitch * 0.017453292F;
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        return new Vec3((double)(-sinYaw * cosPitch), (double)(-sinPitch), (double)(cosYaw * cosPitch));
    }

    public static boolean isRotationFacingTarget(float yaw, float pitch, Entity target, double range) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = getLookVector(yaw, pitch);
        Vec3 end = eyes.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        MovingObjectPosition mop = target.getEntityBoundingBox().calculateIntercept(eyes, end);
        return mop != null;
    }

    public static double getOriginReach(AxisAlignedBB boundingBox, float yaw, float pitch) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = getLookVector(yaw, pitch);
        double maxDist = mc.playerController.getBlockReachDistance() + 8.0D;
        Vec3 end = eyes.addVector(look.xCoord * maxDist, look.yCoord * maxDist, look.zCoord * maxDist);
        MovingObjectPosition mop = boundingBox.calculateIntercept(eyes, end);
        if (mop == null || mop.hitVec == null) {
            if (boundingBox.isVecInside(eyes)) return 0.0D;
            return Double.MAX_VALUE;
        }
        return eyes.distanceTo(mop.hitVec);
    }

    public static Entity rayTraceEntity(float yaw, float pitch, double range, double hBoxExpand, double vBoxExpand) {
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = getLookVector(yaw, pitch);
        Vec3 end = eyes.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        AxisAlignedBB searchBox = mc.thePlayer.getEntityBoundingBox()
                .addCoord(look.xCoord * range, look.yCoord * range, look.zCoord * range)
                .expand(1.0D, 1.0D, 1.0D);
        List<Entity> candidates = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer, searchBox,
                Predicates.and(EntitySelectors.NOT_SPECTATING, e -> e.canBeCollidedWith()));
        Entity closest = null;
        double closestDist = range;
        for (Entity entity : candidates) {
            if (entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity) continue;
            double border = entity.getCollisionBorderSize();
            AxisAlignedBB hitBox = entity.getEntityBoundingBox().expand(border + hBoxExpand, border + vBoxExpand, border + hBoxExpand);
            MovingObjectPosition mop = hitBox.calculateIntercept(eyes, end);
            if (hitBox.isVecInside(eyes)) {
                if (closestDist >= 0.0D) {
                    closest = entity;
                    closestDist = 0.0D;
                }
            } else if (mop != null) {
                double dist = eyes.distanceTo(mop.hitVec);
                if (dist < closestDist || closestDist == 0.0D) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    public static Vec3 getBestHitVec(AxisAlignedBB boundingBox) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        double ex = MathHelper.clamp_double(eyePos.xCoord, boundingBox.minX, boundingBox.maxX);
        double ey = MathHelper.clamp_double(eyePos.yCoord, boundingBox.minY, boundingBox.maxY);
        double ez = MathHelper.clamp_double(eyePos.zCoord, boundingBox.minZ, boundingBox.maxZ);
        return new Vec3(ex, ey, ez);
    }

    public static Vec3 getStableHitVec(AxisAlignedBB boundingBox, double distance) {
        Vec3 bestHit = getBestHitVec(boundingBox);
        if (distance < 2.5) {
            Vec3 center = new Vec3(
                    (boundingBox.minX + boundingBox.maxX) / 2.0,
                    (boundingBox.minY + boundingBox.maxY) / 2.0,
                    (boundingBox.minZ + boundingBox.maxZ) / 2.0
            );
            double blend = Math.max(0.0, (2.5 - distance) / 2.5) * 0.5;
            return new Vec3(
                    bestHit.xCoord * (1.0 - blend) + center.xCoord * blend,
                    bestHit.yCoord * (1.0 - blend) + center.yCoord * blend,
                    bestHit.zCoord * (1.0 - blend) + center.zCoord * blend
            );
        }
        return bestHit;
    }

    public static Vec3 getSmartHitVec(AxisAlignedBB boundingBox, float currentYaw, float currentPitch) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = getLookVector(currentYaw, currentPitch);
        Vec3 endPos = eyePos.addVector(lookVec.xCoord * 6.0, lookVec.yCoord * 6.0, lookVec.zCoord * 6.0);
        MovingObjectPosition mop = boundingBox.calculateIntercept(eyePos, endPos);
        if (mop != null && mop.hitVec != null) {
            return mop.hitVec;
        }
        return getBestHitVec(boundingBox);
    }

    public static MovingObjectPosition getMouseOverFromRotation(float yaw, float pitch) {
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        double d0 = mc.playerController.getBlockReachDistance();
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = getLookVector(yaw, pitch);
        Vec3 end = eyes.addVector(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0);
        MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(eyes, end, false, false, true);
        double d1 = d0;
        boolean flag = false;
        if (mc.playerController.extendedReach()) {
            d0 = 6.0D;
            d1 = 6.0D;
        } else if (d0 > 3.0D) {
            flag = true;
        }
        end = eyes.addVector(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0);
        if (blockHit != null && blockHit.hitVec != null) {
            d1 = eyes.distanceTo(blockHit.hitVec);
        }
        AxisAlignedBB searchBox = mc.thePlayer.getEntityBoundingBox()
                .addCoord(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0)
                .expand(1.0D, 1.0D, 1.0D);
        List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer, searchBox,
                Predicates.and(EntitySelectors.NOT_SPECTATING, e -> e.canBeCollidedWith()));
        Entity pointedEntity = null;
        Vec3 hitVec = null;
        double d2 = d1;
        for (Entity entity : list) {
            if (entity == mc.thePlayer.ridingEntity) continue;
            float border = entity.getCollisionBorderSize();
            AxisAlignedBB box = entity.getEntityBoundingBox().expand(border, border, border);
            MovingObjectPosition mop = box.calculateIntercept(eyes, end);
            if (box.isVecInside(eyes)) {
                if (d2 >= 0.0D) {
                    pointedEntity = entity;
                    hitVec = mop == null ? eyes : mop.hitVec;
                    d2 = 0.0D;
                }
            } else if (mop != null) {
                double d3 = eyes.distanceTo(mop.hitVec);
                if (d3 < d2 || d2 == 0.0D) {
                    pointedEntity = entity;
                    hitVec = mop.hitVec;
                    d2 = d3;
                }
            }
        }
        if (pointedEntity != null && flag && eyes.distanceTo(hitVec) > 3.0D) {
            pointedEntity = null;
            mc.pointedEntity = null;
            return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS,
                    hitVec, null, new BlockPos(hitVec));
        }
        if (pointedEntity != null && (d2 < d1 || blockHit == null)) {
            mc.pointedEntity = pointedEntity;
            return new MovingObjectPosition(pointedEntity, hitVec);
        }
        if (blockHit != null) {
            return blockHit;
        }
        return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, end, null, new BlockPos(end));
    }
}
