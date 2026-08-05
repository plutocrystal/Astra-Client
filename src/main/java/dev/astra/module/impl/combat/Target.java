package dev.astra.module.impl.combat;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import org.lwjgl.input.Keyboard;

public class Target extends Module {

    public static BooleanValue players = new BooleanValue("Players", true);
    public static BooleanValue mobs = new BooleanValue("Mobs", false);
    public static BooleanValue animals = new BooleanValue("Animals", false);
    public static BooleanValue invisible = new BooleanValue("Invisible", false);
    public static BooleanValue dead = new BooleanValue("Dead", false);

    public Target() {
        super("Target", Keyboard.KEY_NONE, Category.COMBAT);
        addValues(players, mobs, animals, invisible, dead);
    }
}