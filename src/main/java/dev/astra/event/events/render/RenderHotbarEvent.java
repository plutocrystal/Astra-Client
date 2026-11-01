package dev.astra.event.events.render;

import dev.astra.event.Event;
import net.minecraft.client.gui.ScaledResolution;

public class RenderHotbarEvent extends Event {
    private final ScaledResolution scaledResolution;

    private final float partialTicks;

    public RenderHotbarEvent(ScaledResolution scaledResolution, float partialTicks) {
        this.scaledResolution = scaledResolution;
        this.partialTicks = partialTicks;
    }

    public ScaledResolution getScaledResolution() {
        return scaledResolution;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
