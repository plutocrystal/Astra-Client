package dev.astra.module.impl.player;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class MultiActions extends Module {

    public MultiActions() {
        super("MultiActions", Keyboard.KEY_NONE, Category.PLAYER);
    }
}