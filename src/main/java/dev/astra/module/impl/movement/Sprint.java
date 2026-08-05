package dev.astra.module.impl.movement;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class Sprint extends Module {

    public BooleanValue omni = new BooleanValue("Omni", false);

    public Sprint() {
        super("Sprint", Keyboard.KEY_NONE, Category.MOVEMENT);
        addValues(omni);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (omni.getValue()) {
            if ((mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0) 
                && !mc.thePlayer.isUsingItem() 
                && !mc.thePlayer.isCollidedHorizontally 
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6) {
                
                mc.thePlayer.setSprinting(true);
            }
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
    }
}