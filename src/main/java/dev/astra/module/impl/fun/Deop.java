package dev.astra.module.impl.fun;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.utils.rotation.RotationManager;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Deop extends Module {

    public static Deop INSTANCE;

    public final NumberValue speed = new NumberValue("Speed", 50.0, 1.0, 180.0, 1.0);
    public final NumberValue pitch = new NumberValue("Pitch", 90.0, -90.0, 90.0, 1.0);
    public final ModeValue moveFix = new ModeValue("MoveFix", "STRICT", "NONE", "STRICT", "SILENT");

    private float currentYaw = 0.0f;

    public Deop() {
        super("Deop", Keyboard.KEY_NONE, Category.FUN);
        INSTANCE = this;
        addValues(speed, pitch, moveFix);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        currentYaw += (float) (double) speed.getValue();
        if (currentYaw > 180.0f) {
            currentYaw -= 360.0f;
        }

        float targetYaw = MathHelper.wrapAngleTo180_float(currentYaw);
        float targetPitch = (float) (double) pitch.getValue();

        RotationManager.MoveFix fixMode;
        switch (moveFix.getValue()) {
            case "STRICT":
                fixMode = RotationManager.MoveFix.STRICT;
                break;
            case "SILENT":
                fixMode = RotationManager.MoveFix.SILENT;
                break;
            default:
                fixMode = RotationManager.MoveFix.NONE;
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