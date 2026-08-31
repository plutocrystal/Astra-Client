package dev.astra.event.events.player;

import dev.astra.event.Event;

public class PostUpdateEvent extends Event {
    private float yaw;

    private float pitch;

    private double posY;

    private boolean onGround;

    public PostUpdateEvent(float yaw, float pitch, double posY, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.posY = posY;
        this.onGround = onGround;
    }

    public float getYaw() { return yaw; }

    public float getPitch() { return pitch; }

    public double getPosY() { return posY; }

    public boolean isOnGround() { return onGround; }
}
