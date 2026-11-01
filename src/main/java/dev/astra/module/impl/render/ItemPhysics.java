package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

public class ItemPhysics extends Module {
    public static ItemPhysics INSTANCE;

    public static ModeValue mode;

    public static NumberValue scale;

    public static BooleanValue showCount;

    public static BooleanValue showDurability;

    public static BooleanValue item;

    public static BooleanValue nameAutoScale;

    public static NumberValue nameSize;

    public ItemPhysics() {
        super("ItemPhysics", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        mode = new ModeValue("Mode", "Default", "Default", "Physics", "1.7");
        scale = new NumberValue("Size", 1.0, 0.1, 3.0, 0.1);
        showCount = new BooleanValue("Count", true);
        showDurability = new BooleanValue("Durability", false);
        item = new BooleanValue("Item", false);
        nameAutoScale = new BooleanValue("NameAutoScale", false);
        nameSize = new NumberValue("NameSize", 1.0, 0.5, 2.0, 0.1);
        addValues(mode, scale, showCount, showDurability, item, nameAutoScale, nameSize);
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    public boolean isPhysicsMode() {
        return this.isToggle() && mode.is("Physics");
    }

    public boolean is17Mode() {
        return this.isToggle() && mode.is("1.7");
    }

    public float getScale() {
        return this.isToggle() ? scale.getValue().floatValue() : 1.0f;
    }

    public boolean isCountEnabled() {
        return this.isToggle() && showCount.getValue();
    }

    public boolean isDurabilityEnabled() {
        return this.isToggle() && showDurability.getValue();
    }

    public boolean isItemNameEnabled() {
        return this.isToggle() && item.getValue();
    }

    public static int get17BlockCount(final ItemStack stack) {
        if (stack.stackSize > 40) {
            return 5;
        } else if (stack.stackSize > 20) {
            return 4;
        } else if (stack.stackSize > 5) {
            return 3;
        } else if (stack.stackSize > 1) {
            return 2;
        }
        return 1;
    }

    public static int get17FlatCount(final ItemStack stack) {
        if (stack.stackSize < 2) {
            return 1;
        } else if (stack.stackSize < 16) {
            return 2;
        } else if (stack.stackSize < 32) {
            return 3;
        }
        return 4;
    }
}