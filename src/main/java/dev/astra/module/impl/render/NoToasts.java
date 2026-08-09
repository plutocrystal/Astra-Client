package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class NoToasts extends Module {

    public static NoToasts INSTANCE;

    public NoToasts() {
        super("NoToasts", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
    }
}