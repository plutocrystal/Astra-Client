package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class NoClickDelay extends Module {

    public NoClickDelay() {
        super("NoClickDelay", Keyboard.KEY_NONE, Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        mc.leftClickCounter = 0;
    }
}