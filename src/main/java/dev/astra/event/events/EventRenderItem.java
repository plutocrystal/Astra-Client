package dev.astra.event.events;

import dev.astra.event.Event;

public class EventRenderItem extends Event {
    private final float equipProgress;
    private final float swingProgress;

    public EventRenderItem(float equipProgress, float swingProgress) {
        this.equipProgress = equipProgress;
        this.swingProgress = swingProgress;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public float getSwingProgress() {
        return swingProgress;
    }
}