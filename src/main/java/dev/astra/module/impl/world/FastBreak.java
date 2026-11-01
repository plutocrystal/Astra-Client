package dev.astra.module.impl.world;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

public class FastBreak extends Module {
    public final NumberValue speed = new NumberValue("Speed", 2.0, 1.0, 5.0, 0.1);

    public final BooleanValue noBreakDelay = new BooleanValue("NoBreakDelay", true);

    public FastBreak() {
        super("FastBreak", Keyboard.KEY_NONE, Category.WORLD);
        addValues(speed, noBreakDelay);
    }
}