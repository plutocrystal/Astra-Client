package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class Rotation extends Module {

    public Rotation() {
        super("Rotation", Keyboard.KEY_NONE, Category.RENDER);
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.visualActive = false;
        }
    }
}