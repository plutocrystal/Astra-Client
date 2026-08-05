package dev.astra.module.impl.misc;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class AutoWalk extends Module {

    public AutoWalk() {
        super("AutoWalk", Keyboard.KEY_NONE, Category.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null) return;
        // 模拟按下 W 键
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // 关闭模块时，根据键盘实际状态恢复按键，防止关了模块还在走
        if (mc.thePlayer != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        }
    }
}