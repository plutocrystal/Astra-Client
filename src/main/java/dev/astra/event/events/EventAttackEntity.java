package dev.astra.event.events;

import dev.astra.event.Event;
import net.minecraft.entity.Entity;

public class EventAttackEntity extends Event {
    private final Entity target;

    public EventAttackEntity(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}