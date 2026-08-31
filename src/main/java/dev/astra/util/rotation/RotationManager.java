package dev.astra.util.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationManager {
    public enum MoveFix {
        None, Strict, Silent
    }

    public enum RotationMode {
        None, LockView, Silent
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

    private static MoveFix moveFix = MoveFix.None;

    private static RotationMode rotationMode = RotationMode.Silent;

    public static void setRotation(float yaw, float pitch, MoveFix fix) {
        setRotation(yaw, pitch, fix, RotationMode.Silent);
    }

    public static MoveFix parseMoveFix(String value) {
        if ("Strict".equals(value)) return MoveFix.Strict;
        if ("Silent".equals(value)) return MoveFix.Silent;
        return MoveFix.None;
    }

    public static RotationMode parseRotationMode(String value) {
        if ("LockView".equals(value)) return RotationMode.LockView;
        if ("Silent".equals(value)) return RotationMode.Silent;
        return RotationMode.None;
    }

    public static void setRotation(float yaw, float pitch, MoveFix fix, RotationMode mode) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (mode == RotationMode.None) {
            clearRotation();
            return;
        }
        yaw = MathHelper.wrapAngleTo180_float(yaw);
        if (pitch > 90.0F) pitch = 90.0F;
        if (pitch < -90.0F) pitch = -90.0F;
        if (!initialized) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
            prevYaw = serverYaw;
            prevPitch = serverPitch;
            bodyYaw = serverYaw;
            prevBodyYaw = bodyYaw;
            initialized = true;
        } else {
            prevYaw = serverYaw;
            prevPitch = serverPitch;
            prevBodyYaw = bodyYaw;
        }
        serverYaw += MathHelper.wrapAngleTo180_float(yaw - serverYaw);
        serverPitch = pitch;
        active = true;
        moveFix = fix;
        rotationMode = mode;
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
        moveFix = MoveFix.None;
        rotationMode = RotationMode.Silent;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.visualActive = false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static MoveFix getMoveFix() {
        return moveFix;
    }

    public static RotationMode getRotationMode() {
        return rotationMode;
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
