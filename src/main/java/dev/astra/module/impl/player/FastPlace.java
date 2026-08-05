package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.Render3DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

public class FastPlace extends Module {

    public final NumberValue delay = new NumberValue("Delay", 0.0, 0.0, 4.0, 1.0);

    public FastPlace() {
        super("FastPlace", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(delay);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        int targetDelay = delay.getValue().intValue();
        if (mc.rightClickDelayTimer > targetDelay) {
            mc.rightClickDelayTimer = targetDelay;
        }
    }
}