package dev.astra.module.impl.movement;

import dev.astra.event.EventHandler;
import dev.astra.event.events.attack.KeepSprintEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;

public class KeepSprint extends Module {
    public KeepSprint() {
        super("KeepSprint", 0, Category.MOVEMENT);
    }

    @EventHandler
    public void onKeepSprint(KeepSprintEvent event) {
        if (mc.thePlayer != null && mc.thePlayer.isSprinting()) {
            event.setCancelled(true);
        }
    }
}
