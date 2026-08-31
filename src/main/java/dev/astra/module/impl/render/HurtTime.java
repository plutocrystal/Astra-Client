package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ColorValue;
import org.lwjgl.input.Keyboard;

public class HurtTime extends Module {
    public static HurtTime INSTANCE;

    public ColorValue hurtColor = new ColorValue("HurtColor", 0x4DFF0000);

    public HurtTime() {
        super("HurtTime", Keyboard.KEY_NONE, Category.RENDER);
        addValues(hurtColor);
        INSTANCE = this;
    }

    public static float[] getHurtColor() {
        if (INSTANCE == null || !INSTANCE.isToggle()) {
            return new float[]{1.0F, 0.0F, 0.0F, 0.3F};
        }
        int color = INSTANCE.hurtColor.getValue();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        return new float[]{r, g, b, a};
    }
}
