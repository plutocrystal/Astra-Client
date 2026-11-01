package dev.astra.module.impl.render;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.SoundUtil;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.client.Minecraft;

public class ClientSettings extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean ACTIVE = false;

    private final ModeValue toggleSound = new ModeValue("ToggleSound", "Piston", "None", "Piston", "ClickButton", "ClickWoodButton", "Sigma", "Fdp");

    private final NumberValue soundVolume = new NumberValue("SoundVolume", 1.0, 0.0, 1.0, 0.05);

    private final ModeValue buttonSound = new ModeValue("ButtonSound", "Vanilla", "Vanilla", "Astra");

    private final ModeValue buttonStyle = new ModeValue("ButtonStyle", "Vanilla", "Vanilla", "Augustus");

    private final ColorValue buttonColor = new ColorValue("ButtonColor", 0xFF169BEB);

    private final NumberValue buttonCornerRadius = new NumberValue("ButtonCornerRadius", 4.0, 0.0, 10.0, 0.5);

    public ClientSettings() {
        super("ClientSettings", 0, Category.RENDER);
        addValues(toggleSound, soundVolume, buttonSound, buttonStyle, buttonColor, buttonCornerRadius);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ACTIVE = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ACTIVE = false;
    }

    @Override
    public void toggle() {
        if (!this.isToggle()) {
            super.toggle();
        }
    }

    public static void onModuleToggle(Module module, boolean enabled) {
        if (!ACTIVE || module == null || mc.thePlayer == null) return;
        ClientSettings cs = Main.moduleManager.getModuleByClass(ClientSettings.class);
        if (cs == null) return;
        String sound = cs.toggleSound.getValue();
        if (sound.equals("None")) return;
        float volume = cs.soundVolume.getValue().floatValue();
        if (sound.equals("Piston")) {
            SoundUtil.playSound(enabled ? "tile.piston.out" : "tile.piston.in", volume);
            return;
        }
        if (sound.equals("Sigma")) {
            SoundUtil.playSound(enabled ? "astra.snd:astra.sigma.enable" : "astra.snd:astra.sigma.disable", volume);
            return;
        }
        if (sound.equals("Fdp")) {
            SoundUtil.playSound(enabled ? "astra.snd:astra.fdp.enable" : "astra.snd:astra.fdp.disable", volume);
            return;
        }
        String name;
        switch (sound) {
            case "ClickButton": name = "random.click"; break;
            case "ClickWoodButton": name = "random.wood_click"; break;
            default: return;
        }
        SoundUtil.playSound(name, volume);
    }

    public static boolean useAugustusStyle() {
        ClientSettings cs = Main.moduleManager.getModuleByClass(ClientSettings.class);
        return cs != null && cs.buttonStyle.is("Augustus");
    }

    public static boolean useAstraButtonSound() {
        ClientSettings cs = Main.moduleManager.getModuleByClass(ClientSettings.class);
        return cs != null && cs.buttonSound.is("Astra");
    }

    public static float getSoundVolume() {
        ClientSettings cs = Main.moduleManager.getModuleByClass(ClientSettings.class);
        return cs != null ? cs.soundVolume.getValue().floatValue() : 1.0F;
    }

    public static int getButtonColor() {
        ClientSettings cs = Main.moduleManager.getModuleByClass(ClientSettings.class);
        return cs != null ? cs.buttonColor.getValue() : 0xFF169BEB;
    }

    public static float getButtonCornerRadius() {
        ClientSettings cs = Main.moduleManager.getModuleByClass(ClientSettings.class);
        return cs != null ? cs.buttonCornerRadius.getValue().floatValue() : 4.0F;
    }
}
