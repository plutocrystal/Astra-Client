package dev.astra.module.impl.player;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class NoPush extends Module {

    public static NoPush INSTANCE;

    public NoPush() {
        super("NoPush", Keyboard.KEY_NONE, Category.PLAYER);
        INSTANCE = this;
    }
}