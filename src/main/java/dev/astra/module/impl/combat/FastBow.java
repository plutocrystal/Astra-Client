package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.lwjgl.input.Keyboard;

public class FastBow extends Module {

    private final NumberValue packets = new NumberValue("Packets", 20, 3, 20, 1);

    public FastBow() {
        super("FastBow", Keyboard.KEY_NONE, Category.COMBAT);
        addValues(packets);
    }

    @Override
    public String getStatus() {
        return String.valueOf(packets.getValue().intValue());
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!mc.thePlayer.isUsingItem()) return;

        ItemStack currentItem = mc.thePlayer.inventory.getCurrentItem();

        if (currentItem != null && currentItem.getItem() instanceof ItemBow) {
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, currentItem, 0.0f, 0.0f, 0.0f));

            float yaw = mc.thePlayer.rotationYaw;
            float pitch = mc.thePlayer.rotationPitch;

            int count = packets.getValue().intValue();
            for (int i = 0; i < count; i++) {
                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, true));
            }

            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            mc.thePlayer.setItemInUseCount(currentItem.getMaxItemUseDuration() - 1);
        }
    }
}
