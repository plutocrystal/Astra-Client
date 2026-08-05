package dev.astra.utils.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationManager {

    public enum MoveFix {
        NONE, STRICT, SILENT
    }

    private static float serverYaw;
    private static float serverPitch;
    private static float prevYaw;
    private static float prevPitch;
    private static boolean active = false;
    private static float bodyYaw;
    private static float prevBodyYaw;
    private static boolean initialized = false;
    public static boolean showVisual = false;
    private static MoveFix moveFix = MoveFix.NONE;

    public static void setRotation(float yaw, float pitch, MoveFix fix) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        yaw = MathHelper.wrapAngleTo180_float(yaw);
        if (pitch > 90.0F) pitch = 90.0F;
        if (pitch < -90.0F) pitch = -90.0F;

        if (!initialized) {
            prevYaw = mc.thePlayer.rotationYaw;
            prevPitch = mc.thePlayer.rotationPitch;
            bodyYaw = mc.thePlayer.rotationYaw;
            prevBodyYaw = bodyYaw;
            initialized = true;
        } else {
            prevYaw = serverYaw;
            prevPitch = serverPitch;
            prevBodyYaw = bodyYaw;
        }

        serverYaw = yaw;
        serverPitch = pitch;
        active = true;
        moveFix = fix;

        double d0 = MathHelper.wrapAngleTo180_double(serverYaw - bodyYaw);
        bodyYaw += d0 * 0.3D;
        double d1 = MathHelper.wrapAngleTo180_double(serverYaw - bodyYaw);
        if (d1 < -75.0D) d1 = -75.0D;
        if (d1 > 75.0D) d1 = 75.0D;
        bodyYaw = (float)((double)serverYaw - d1);
    }

    public static void clearRotation() {
        active = false;
        initialized = false;
        moveFix = MoveFix.NONE;
    }

    public static float[] fixMovement(float forward, float strafe, float visualYaw) {
        if (!active || moveFix == MoveFix.NONE) {
            return new float[]{forward, strafe};
        }

        if (forward == 0.0F && strafe == 0.0F) {
            return new float[]{0.0F, 0.0F};
        }

        float diffRad = (serverYaw - visualYaw) * 0.017453292F;
        float cosDiff = MathHelper.cos(diffRad);
        float sinDiff = MathHelper.sin(diffRad);

        float newStrafe = strafe * cosDiff - forward * sinDiff;
        float newForward = strafe * sinDiff + forward * cosDiff;

        return new float[]{newForward, newStrafe};
    }

    public static boolean isActive() {
        return active;
    }

    public static MoveFix getMoveFix() {
        return moveFix;
    }

    public static float getYaw() {
        return serverYaw;
    }

    public static float getPitch() {
        return serverPitch;
    }

    public static float getPrevYaw() {
        return prevYaw;
    }

    public static float getPrevPitch() {
        return prevPitch;
    }

    public static float getBodyYaw() {
        return bodyYaw;
    }

    public static float getPrevBodyYaw() {
        return prevBodyYaw;
    }
}