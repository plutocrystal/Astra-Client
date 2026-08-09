package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class NoRender extends Module {

    public static NoRender INSTANCE;

    public NoRender() {
        super("NoRender", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
    }
}