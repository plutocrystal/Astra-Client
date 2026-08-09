package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class Chat extends Module {

    public static Chat INSTANCE;

    public Chat() {
        super("Chat", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
    }
}