package dev.astra.module.impl.movement;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class NoJumpDelay extends Module {

    public NoJumpDelay() {
        super("NoJumpDelay", Keyboard.KEY_NONE, Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        mc.thePlayer.jumpTicks = 0;
    }
}