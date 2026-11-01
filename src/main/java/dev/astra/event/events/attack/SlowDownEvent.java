package dev.astra.event.events.attack;

import dev.astra.event.Event;
import net.minecraft.entity.Entity;

public class SlowDownEvent extends Event {
    private final Entity target;

    private int ticks;

    public SlowDownEvent(Entity target, int ticks) {
        this.target = target;
        this.ticks = ticks;
    }

    public Entity getTarget() {
        return target;
    }

    public int getTicks() {
        return ticks;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }
}
