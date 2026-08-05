package dev.astra.utils.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float applyGCD(float target, float current) {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;
        float gcdVal = f1 * 0.15F;
        float diff = target - current;
        return current + Math.round(diff / gcdVal) * gcdVal;
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
}