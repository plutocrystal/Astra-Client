package dev.astra.module.impl.movement;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;

public class KeepSprint extends Module {
    public static NumberValue slowDown;

    public KeepSprint() {
        super("KeepSprint", 0, Category.MOVEMENT);
        slowDown = new NumberValue("SlowDown", 0.6, 0.0, 1.0, 0.01);
        addValues(slowDown);
    }
}
