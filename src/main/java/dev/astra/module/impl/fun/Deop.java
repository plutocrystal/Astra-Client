package dev.astra.module.impl.fun;

import dev.astra.event.EventHandler;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.rotation.RotationManager;
import dev.astra.util.rotation.RotationUtils;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Deop extends Module {
    public static Deop INSTANCE;

    public final NumberValue speed = new NumberValue("Speed", 50.0, 1.0, 180.0, 1.0);

    public final NumberValue pitch = new NumberValue("Pitch", 90.0, -90.0, 90.0, 1.0);

    public final ModeValue moveFix = new ModeValue("MoveFix", "Strict", "None", "Strict", "Silent");

    private float currentYaw = 0.0f;

    public Deop() {
        super("Deop", Keyboard.KEY_NONE, Category.FUN);
        INSTANCE = this;
        addValues(speed, pitch, moveFix);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        currentYaw += (float) (double) speed.getValue();
        if (currentYaw > 180.0f) {
            currentYaw -= 360.0f;
        }
        float targetYaw = MathHelper.wrapAngleTo180_float(currentYaw);
        float targetPitch = (float) (double) pitch.getValue();
        targetYaw = RotationUtils.applyGCD(targetYaw, mc.thePlayer.rotationYaw);
        targetPitch = RotationUtils.applyGCD(targetPitch, mc.thePlayer.rotationPitch);
        RotationManager.MoveFix fixMode;
        switch (moveFix.getValue()) {
            case "Strict":
                fixMode = RotationManager.MoveFix.Strict;
                break;
            case "Silent":
                fixMode = RotationManager.MoveFix.Silent;
                break;
            default:
                fixMode = RotationManager.MoveFix.None;
                break;
        }
        RotationManager.setRotation(targetYaw, targetPitch, fixMode);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        RotationManager.clearRotation();
    }
}
