package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import org.lwjgl.input.Keyboard;

public class Scoreboard extends Module {

    public static Scoreboard INSTANCE;

    public Scoreboard() {
        super("Scoreboard", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
    }
}