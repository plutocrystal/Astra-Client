package dev.astra.module.impl.fun;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;

public class CustomTNT extends Module {
    public static CustomTNT INSTANCE;

    public final NumberValue power = new NumberValue("Power", 10.0, 1.0, 10000.0, 1.0);

    public CustomTNT() {
        super("CustomTNT", 0, Category.FUN);
        INSTANCE = this;
        addValues(power);
    }
}
