package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.Render3DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemSword;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class AutoClick extends Module {

    public final RangeSelectionNumberValue cps = new RangeSelectionNumberValue("CPS", 5.0, 8.0, 1.0, 20.0, 1.0);
    public final BooleanValue leftClick = new BooleanValue("LeftClick", true);
    public final BooleanValue onlySword = new BooleanValue("OnlySword", false);

    private long leftLastSwing = 0L;
    private long leftDelay = 100L;

    public AutoClick() {
        super("AutoClick", Keyboard.KEY_NONE, Category.COMBAT);
        addValues(cps, leftClick, onlySword);
    }

    @Override
    public void onEnable() {
        leftLastSwing = System.currentTimeMillis();
        leftDelay = getRandomDelay();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        if (onlySword.getValue()) {
            if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return;
            }
        }

        if (mc.gameSettings.keyBindAttack.isKeyDown() && leftClick.getValue() &&
            System.currentTimeMillis() - leftLastSwing >= leftDelay && mc.playerController.curBlockDamageMP == 0F) {
            
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());

            leftLastSwing = System.currentTimeMillis();
            leftDelay = getRandomDelay();
        }
    }

    private long getRandomDelay() {
        double minCps = cps.getMinValue();
        double maxCps = cps.getMaxValue();
        
        int min = (int) Math.min(minCps, maxCps);
        int max = (int) Math.max(minCps, maxCps);
        if (max <= 0) max = 1;
        if (min <= 0) min = 1;
        
        int targetCps = min + ThreadLocalRandom.current().nextInt(max - min + 1);
        return (long) (1000.0 / targetCps);
    }
}