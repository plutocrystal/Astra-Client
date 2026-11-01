package dev.astra.util.inventory;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.packet.PacketReceiveEvent;
import dev.astra.event.events.packet.PacketSendEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;

public final class LBInventoryUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static boolean serverOpenInventory = false;

    public static boolean isFirstInventoryClick = true;

    public static long timeSinceClosedInventory = 0L;

    private LBInventoryUtils() {
    }

    public static boolean isServerOpenInventory() {
        return serverOpenInventory;
    }

    public static void setServerOpenInventory(boolean value) {
        if (value != serverOpenInventory && mc.thePlayer != null) {
            if (value) {
                mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            } else {
                mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.openContainer != null ? mc.thePlayer.openContainer.windowId : 0));
            }
            serverOpenInventory = value;
        }
    }

    public static Integer toHotbarIndex(int index, int stacksSize) {
        int parsed = index - stacksSize + 9;
        if (parsed >= 0 && parsed <= 8) {
            return parsed;
        }
        return null;
    }

    public static boolean hasSpaceInInventory() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.inventory.getFirstEmptyStack() != -1;
    }

    public static int countSpaceInInventory() {
        if (mc.thePlayer == null) return 0;
        int count = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack == null) count++;
        }
        return count;
    }

    public static boolean passedPostInventoryCloseDelay(int postInventoryCloseDelay) {
        return System.currentTimeMillis() - timeSinceClosedInventory >= postInventoryCloseDelay;
    }

    public static void onWorldChange() {
        SilentHotbar.onWorldChange();
        serverOpenInventory = false;
    }

    public static class PacketListener {
        @EventHandler
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacket() instanceof C08PacketPlayerBlockPlacement
                    || event.getPacket() instanceof C0EPacketClickWindow) {
                if (event.getPacket() instanceof C0EPacketClickWindow) {
                    isFirstInventoryClick = false;
                }
            } else if (event.getPacket() instanceof C16PacketClientStatus) {
                C16PacketClientStatus packet = (C16PacketClientStatus) event.getPacket();
                if (packet.getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    if (serverOpenInventory) {
                        event.setCancelled(true);
                    } else {
                        isFirstInventoryClick = true;
                        serverOpenInventory = true;
                    }
                }
            } else if (event.getPacket() instanceof C0DPacketCloseWindow) {
                isFirstInventoryClick = false;
                serverOpenInventory = false;
                timeSinceClosedInventory = System.currentTimeMillis();
            }
        }
        @EventHandler
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.isCancelled()) return;
            if (event.getPacket() instanceof S2EPacketCloseWindow || event.getPacket() instanceof S2DPacketOpenWindow) {
                isFirstInventoryClick = false;
                serverOpenInventory = false;
                timeSinceClosedInventory = System.currentTimeMillis();
            }
        }
    }
}
