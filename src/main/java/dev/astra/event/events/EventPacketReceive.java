package dev.astra.event.events;

import dev.astra.event.Event;
import net.minecraft.network.Packet;

public class EventPacketReceive extends Event {
    private final Packet<?> packet;

    public EventPacketReceive(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}