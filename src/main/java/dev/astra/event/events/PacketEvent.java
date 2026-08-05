package dev.astra.event.events;

import dev.astra.event.Event;
import net.minecraft.network.Packet;

public class PacketEvent extends Event {
    private final Packet<?> packet;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}