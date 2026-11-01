package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

public class NoRender extends Module {
    public static NoRender INSTANCE;

    public BooleanValue blockOverlay = new BooleanValue("BlockOverlay", true);

    public BooleanValue waterOverlay = new BooleanValue("WaterOverlay", false);

    public BooleanValue fireOverlay = new BooleanValue("FireOverlay", false);

    public BooleanValue helmet = new BooleanValue("Helmet", false);

    public BooleanValue chestplate = new BooleanValue("Chestplate", false);

    public BooleanValue leggings = new BooleanValue("Leggings", false);

    public BooleanValue boots = new BooleanValue("Boots", false);
    public BooleanValue hurtCam = new BooleanValue("HurtCam", false);

    public BooleanValue portalOverlay = new BooleanValue("Portal", true);

    public BooleanValue portalWobble = new BooleanValue("Wobble", true);

    public BooleanValue nausea = new BooleanValue("Nausea", true);

    public BooleanValue blindness = new BooleanValue("Blindness", true);

    public BooleanValue pumpkin = new BooleanValue("Pumpkin", true);

    public BooleanValue viewBobbing = new BooleanValue("ViewBobbing", true);

    public NoRender() {
        super("NoRender", Keyboard.KEY_NONE, Category.RENDER);
        addValues(blockOverlay, waterOverlay, fireOverlay, helmet, chestplate, leggings, boots, hurtCam, portalOverlay, portalWobble, nausea, blindness, pumpkin, viewBobbing);
        INSTANCE = this;
    }

    public static boolean shouldHideBlockOverlay() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.blockOverlay.isEnabled();
    }

    public static boolean hideWaterOverlay() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.waterOverlay.isEnabled();
    }

    public static boolean hideFireOverlay() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.fireOverlay.isEnabled();
    }

    public static boolean hideArmorSlot(int armorSlot) {
        if (INSTANCE == null || !INSTANCE.isToggle()) return false;
        switch (armorSlot) {
            case 4: return INSTANCE.helmet.isEnabled();
            case 3: return INSTANCE.chestplate.isEnabled();
            case 2: return INSTANCE.leggings.isEnabled();
            case 1: return INSTANCE.boots.isEnabled();
            default: return false;
        }
    }

    public static boolean hideHurtCam() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.hurtCam.isEnabled();
    }

    public static boolean hidePortalOverlay() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.portalOverlay.isEnabled();
    }

    public static boolean shouldHideWobble() {
        return INSTANCE != null && INSTANCE.isToggle()
                && (INSTANCE.portalWobble.isEnabled()
                    || (INSTANCE.nausea.isEnabled()
                        && Minecraft.getMinecraft().thePlayer != null
                        && Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.confusion)));
    }

    public static boolean hideBlindness() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.blindness.isEnabled();
    }

    public static boolean hidePumpkinOverlay() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.pumpkin.isEnabled();
    }

    public static boolean hideViewBobbing() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.viewBobbing.isEnabled();
    }
}
