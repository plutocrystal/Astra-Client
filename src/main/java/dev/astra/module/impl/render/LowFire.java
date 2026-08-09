package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

public class LowFire extends Module {

    public static LowFire INSTANCE;
    public static NumberValue height;

    public LowFire() {
        super("LowFire", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        height = new NumberValue("Height", 0.0, 0.0, 2.0, 0.05);
        addValues(height);
    }
}