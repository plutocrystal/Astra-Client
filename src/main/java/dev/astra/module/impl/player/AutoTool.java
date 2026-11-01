package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

public class AutoTool extends Module {
    public final BooleanValue onlySneak = new BooleanValue("OnlySneak", false);

    public final BooleanValue switchBack = new BooleanValue("SwitchBack", true);

    private int previousSlot = -1;

    private boolean wasDigging = false;

    public AutoTool() {
        super("AutoTool", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(onlySneak, switchBack);
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && wasDigging) {
            mc.thePlayer.inventory.currentItem = previousSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(previousSlot));
            previousSlot = -1;
            wasDigging = false;
        }
        super.onDisable();
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (onlySneak.getValue() && !mc.thePlayer.isSneaking()) {
            if (wasDigging) {
                mc.thePlayer.inventory.currentItem = previousSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(previousSlot));
                previousSlot = -1;
                wasDigging = false;
            }
            return;
        }
        boolean isAttacking = mc.gameSettings.keyBindAttack.isKeyDown();
        boolean isLookingAtBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
        if (isAttacking && isLookingAtBlock) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            float bestSpeed = 1.0F;
            int bestSlot = mc.thePlayer.inventory.currentItem;
            ItemStack currentStack = mc.thePlayer.getHeldItem();
            if (currentStack != null) {
                bestSpeed = currentStack.getStrVsBlock(block);
            }
            for (int i = 0; i < 9; i++) {
                if (i == mc.thePlayer.inventory.currentItem) continue;
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack == null) continue;
                float speed = stack.getStrVsBlock(block);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
            if (bestSlot != mc.thePlayer.inventory.currentItem) {
                if (!wasDigging) {
                    previousSlot = mc.thePlayer.inventory.currentItem;
                    wasDigging = true;
                }
                mc.thePlayer.inventory.currentItem = bestSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(bestSlot));
            }
        } else {
            if (wasDigging) {
                if (switchBack.getValue()) {
                    mc.thePlayer.inventory.currentItem = previousSlot;
                    mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(previousSlot));
                }
                previousSlot = -1;
                wasDigging = false;
            }
        }
    }
}
