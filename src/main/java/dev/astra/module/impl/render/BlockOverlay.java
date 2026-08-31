package dev.astra.module.impl.render;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;

public class BlockOverlay extends Module {
    public ModeValue mode = new ModeValue("Mode", "Both", "Both", "Fill");

    public ColorValue outlineColor = new ColorValue("OutlineColor", 0x66000000);

    public ColorValue fillColor = new ColorValue("FillColor", 0x40FF0000);

    public BlockOverlay() {
        super("BlockOverlay", 0, Category.RENDER);
        addValues(mode, outlineColor, fillColor);
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    public static BlockOverlay getInstance() {
        return Main.moduleManager.getModuleByClass(BlockOverlay.class);
    }

    public static String getMode() {
        BlockOverlay o = getInstance();
        if (o == null || !o.isToggle()) return "Outline";
        return o.mode.getValue();
    }

    public static int getOutlineRed() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.outlineColor.getRed();
        return 0;
    }

    public static int getOutlineGreen() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.outlineColor.getGreen();
        return 0;
    }

    public static int getOutlineBlue() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.outlineColor.getBlue();
        return 0;
    }

    public static int getOutlineAlpha() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.outlineColor.getAlpha();
        return 102;
    }

    public static int getFillRed() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.fillColor.getRed();
        return 0;
    }

    public static int getFillGreen() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.fillColor.getGreen();
        return 0;
    }

    public static int getFillBlue() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.fillColor.getBlue();
        return 0;
    }

    public static int getFillAlpha() {
        BlockOverlay o = getInstance();
        if (o != null && o.isToggle()) return o.fillColor.getAlpha();
        return 102;
    }
}
