package dev.astra.module.impl.render;

import dev.astra.gui.ClickGuiScreen;
import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class ClickGui extends Module {

    public ClickGui() {
        super("ClickGui", Keyboard.KEY_RSHIFT, Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null && mc.theWorld != null) {
            mc.displayGuiScreen(new ClickGuiScreen());
        }
    }

    @Override
    public void onDisable() {
    }
}