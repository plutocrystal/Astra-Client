package dev.astra.module.impl.movement;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

public class Eagle extends Module {

    public Eagle() {
        super("Eagle", Keyboard.KEY_NONE, Category.MOVEMENT);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.currentScreen != null) {
            return;
        }

        if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
            return;
        }

        if (mc.thePlayer.onGround) {
            double nextX = mc.thePlayer.posX + mc.thePlayer.motionX;
            double nextZ = mc.thePlayer.posZ + mc.thePlayer.motionZ;
            BlockPos pos = new BlockPos(nextX, mc.thePlayer.posY - 1.0, nextZ);
            Block blockUnder = mc.theWorld.getBlockState(pos).getBlock();
            
            mc.gameSettings.keyBindSneak.pressed = (blockUnder == Blocks.air);
        } else {
            mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.gameSettings.keyBindSneak != null) {
            mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        }
    }
}