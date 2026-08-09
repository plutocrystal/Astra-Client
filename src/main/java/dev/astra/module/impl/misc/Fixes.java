package dev.astra.module.impl.misc;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import org.lwjgl.input.Keyboard;

public class Fixes extends Module {

    public static Fixes INSTANCE;

    public final BooleanValue mouseDelayFix = new BooleanValue("MouseDelayFix", true);
    public final BooleanValue hitDelayFix = new BooleanValue("HitDelayFix", false);

    public Fixes() {
        super("Fixes", Keyboard.KEY_NONE, Category.MISC);
        INSTANCE = this;
        addValues(mouseDelayFix, hitDelayFix);
    }
}