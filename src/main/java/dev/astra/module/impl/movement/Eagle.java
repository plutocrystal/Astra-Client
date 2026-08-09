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

        // 只要打开了任何GUI（如背包、聊天框），直接跳过所有逻辑，交给原版处理
        if (mc.currentScreen != null) {
            return;
        }

        // 只有在手上拿着方块时才启用
        if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
            return;
        }

        // 核心逻辑：玩家在地面上时，预测下一步的位置，如果下一步脚下是空气，就强制按下潜行键
        if (mc.thePlayer.onGround) {
            // 预测下一刻的坐标，防止走太快冲出边缘
            double nextX = mc.thePlayer.posX + mc.thePlayer.motionX;
            double nextZ = mc.thePlayer.posZ + mc.thePlayer.motionZ;
            BlockPos pos = new BlockPos(nextX, mc.thePlayer.posY - 1.0, nextZ);
            Block blockUnder = mc.theWorld.getBlockState(pos).getBlock();
            
            mc.gameSettings.keyBindSneak.pressed = (blockUnder == Blocks.air);
        } else {
            // 如果不在地面上（跳跃中等），恢复按键原状
            mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // 关闭模块时，如果玩家没有物理按住Shift，则释放按键
        if (mc.gameSettings.keyBindSneak != null) {
            mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        }
    }
}