package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

public class AutoTool extends Module {

    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", Keyboard.KEY_NONE, Category.PLAYER);
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && previousSlot != -1) {
            mc.thePlayer.inventory.currentItem = previousSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(previousSlot));
            previousSlot = -1;
        }
        super.onDisable();
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();

            float bestSpeed = 1.0F;
            int bestSlot = -1;
            int currentSlot = mc.thePlayer.inventory.currentItem;

            ItemStack currentStack = mc.thePlayer.getHeldItem();
            if (currentStack != null) {
                bestSpeed = currentStack.getStrVsBlock(block);
                bestSlot = currentSlot;
            }

            for (int i = 0; i < 9; i++) {
                if (i == currentSlot) {
                    continue;
                }
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack == null) {
                    continue;
                }
                
                float speed = stack.getStrVsBlock(block);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1 && bestSlot != currentSlot) {
                if (previousSlot == -1) {
                    previousSlot = currentSlot;
                }
                mc.thePlayer.inventory.currentItem = bestSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(bestSlot));
            }
        } else {
            if (previousSlot != -1) {
                mc.thePlayer.inventory.currentItem = previousSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(previousSlot));
                previousSlot = -1;
            }
        }
    }
}