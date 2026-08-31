package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import org.lwjgl.input.Keyboard;

public class TrueSight extends Module {
    public final BooleanValue entities = new BooleanValue("Entities", true);

    public final BooleanValue barriers = new BooleanValue("Barriers", true);

    public TrueSight() {
        super("TrueSight", Keyboard.KEY_NONE, Category.RENDER);
        addValues(entities, barriers);
    }
}
