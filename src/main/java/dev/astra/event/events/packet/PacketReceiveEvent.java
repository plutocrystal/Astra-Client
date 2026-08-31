package dev.astra.event.events.packet;

import dev.astra.event.Event;
import net.minecraft.network.Packet;

public class PacketReceiveEvent extends Event {
    private final Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
