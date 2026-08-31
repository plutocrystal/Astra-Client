package dev.astra.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;

public class PacketUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void send(Packet<?> packet) {
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            mc.thePlayer.sendQueue.addToSendQueue(packet);
        }
    }

    public static void sendSilent(Packet<?> packet) {
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            mc.thePlayer.sendQueue.sendPacketNoEvent(packet);
        }
    }

    @SuppressWarnings("unchecked")
    public static void receiveSilent(Packet<?> packet) {
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.thePlayer.sendQueue);
        }
    }
}
