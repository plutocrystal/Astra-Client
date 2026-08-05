package dev.astra.event.events;

import dev.astra.event.Event;

public class EventPreUpdate extends Event {
    private float yaw;
    private float pitch;
    private double posY;
    private boolean onGround;

    public EventPreUpdate(float yaw, float pitch, double posY, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.posY = posY;
        this.onGround = onGround;
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
}