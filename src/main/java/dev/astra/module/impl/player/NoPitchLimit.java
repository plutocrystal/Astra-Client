package dev.astra.module.impl.player;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import org.lwjgl.input.Keyboard;

public class NoPitchLimit extends Module {

    public static NoPitchLimit INSTANCE;

    public final BooleanValue serverSide = new BooleanValue("ServerSide", true);

    public NoPitchLimit() {
        super("NoPitchLimit", Keyboard.KEY_NONE, Category.PLAYER);
        INSTANCE = this;
        addValues(serverSide);
    }
}