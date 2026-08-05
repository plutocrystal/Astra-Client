package dev.astra.module.impl.misc;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class AutoMine extends Module {

    public AutoMine() {
        super("AutoMine", Keyboard.KEY_NONE, Category.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.playerController == null) return;
        // 模拟按下鼠标左键，保持挖掘状态
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // 关闭模块时，释放鼠标左键状态
        if (mc.thePlayer != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode()));
        }
    }
}