package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ModeValue;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Keyboard;

public class FullBright extends Module {

    public ModeValue mode = new ModeValue("Mode", "Gamma", "Gamma", "Potion");
    private float oldGamma;

    public FullBright() {
        super("FullBright", Keyboard.KEY_NONE, Category.RENDER);
        addValues(mode);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        oldGamma = mc.gameSettings.gammaSetting;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        if (mode.getValue().equals("Gamma")) {
            mc.gameSettings.gammaSetting = oldGamma;
        } else if (mode.getValue().equals("Potion")) {
            mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mode.getValue().equals("Gamma")) {
            mc.gameSettings.gammaSetting = 100.0F;
        } else if (mode.getValue().equals("Potion")) {
            mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 1337, 0));
        }
    }
}