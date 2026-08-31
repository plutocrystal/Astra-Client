package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

public class ItemPhysics extends Module {
    public static ItemPhysics INSTANCE;

    public static ModeValue mode;

    public static NumberValue scale;

    public static BooleanValue showCount = new BooleanValue("Count", false);

    public static BooleanValue showDurability = new BooleanValue("Durability", false);

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    public ItemPhysics() {
        super("ItemPhysics", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        mode = new ModeValue("Mode", "Default", "Default", "Physics", "1.7");
        scale = new NumberValue("Size", 1.0, 0.1, 3.0, 0.1);
        addValues(mode, scale, showCount, showDurability);
    }
}
