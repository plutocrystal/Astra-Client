package dev.astra.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class MoveUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isForwardPressed() {
        if (mc.gameSettings.keyBindForward.isKeyDown() != mc.gameSettings.keyBindBack.isKeyDown()) {
            return true;
        }
        return mc.gameSettings.keyBindLeft.isKeyDown() != mc.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        if (forward < 0.0F) {
            yaw += 180.0F;
        }
        if (strafe != 0.0F) {
            float multiplier = forward == 0.0F ? 1.0F : 0.5F * Math.signum(forward);
            yaw += -90.0F * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static void fixStrafe(float targetYaw) {
        net.minecraft.util.MovementInput input = mc.thePlayer.movementInput;
        float angle = MathHelper.wrapAngleTo180_float(
                adjustYaw(mc.thePlayer.rotationYaw, getForwardValue(), getLeftValue()) - targetYaw + 22.5F);
        switch ((int) (angle + 180.0F) / 45 % 8) {
            case 0:
                input.moveForward = -1.0F;
                input.moveStrafe = 0.0F;
                break;
            case 1:
                input.moveForward = -1.0F;
                input.moveStrafe = 1.0F;
                break;
            case 2:
                input.moveForward = 0.0F;
                input.moveStrafe = 1.0F;
                break;
            case 3:
                input.moveForward = 1.0F;
                input.moveStrafe = 1.0F;
                break;
            case 4:
                input.moveForward = 1.0F;
                input.moveStrafe = 0.0F;
                break;
            case 5:
                input.moveForward = 1.0F;
                input.moveStrafe = -1.0F;
                break;
            case 6:
                input.moveForward = 0.0F;
                input.moveStrafe = -1.0F;
                break;
            case 7:
                input.moveForward = -1.0F;
                input.moveStrafe = -1.0F;
                break;
        }
        if (input.sneak) {
            input.moveForward *= 0.3F;
            input.moveStrafe *= 0.3F;
        }
    }
}
