package dev.astra.event.events.render;

import dev.astra.event.Event;

public class RenderItemEvent extends Event {
    private final float equipProgress;

    private final float swingProgress;

    public RenderItemEvent(float equipProgress, float swingProgress) {
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
