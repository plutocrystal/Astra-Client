package dev.astra.event.events;

import dev.astra.event.Event;
import net.minecraft.network.Packet;

public class EventPacketSend extends Event {
    private final Packet<?> packet;

    public EventPacketSend(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}