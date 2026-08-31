package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

public class Chat extends Module {
    public static Chat INSTANCE;

    public final BooleanValue rounded = new BooleanValue("Rounded", false);

    public final NumberValue radius = new NumberValue("Radius", 3, 0, 8, 1);

    public final NumberValue opacity = new NumberValue("Opacity", 50, 0, 100, 1);

    public final BooleanValue infiniteChat = new BooleanValue("InfiniteChat", false);

    public Chat() {
        super("Chat", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        addValues(rounded, radius, opacity, infiniteChat);
        refreshVisibility();
    }

    @Override
    public void refreshVisibility() {
        radius.setVisible(rounded.isEnabled());
        opacity.setVisible(rounded.isEnabled());
    }
}
