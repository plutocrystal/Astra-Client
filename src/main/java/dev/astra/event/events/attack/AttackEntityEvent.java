package dev.astra.event.events.attack;

import dev.astra.event.Event;
import net.minecraft.entity.Entity;

public class AttackEntityEvent extends Event {
    private final Entity target;

    public AttackEntityEvent(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
