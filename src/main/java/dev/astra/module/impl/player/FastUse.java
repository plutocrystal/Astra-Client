package dev.astra.module.impl.player;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.misc.GameLoopEvent;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.lwjgl.input.Keyboard;

public class FastUse extends Module {

    private final ModeValue mode = new ModeValue("Mode", "NCP", "Instant", "NCP", "AAC", "Custom", "Bug");

    private final NumberValue customDelay = new NumberValue("CustomDelay", 0, 0, 300, 1);

    private final NumberValue customSpeed = new NumberValue("CustomSpeed", 2, 1, 35, 1);

    private final NumberValue customTimer = new NumberValue("CustomTimer", 1.1, 0.5, 2.0, 0.01);

    private final NumberValue dropTick = new NumberValue("DropTick", 10, 1, 30, 1);

    private final BooleanValue noMove = new BooleanValue("NoMove", false);

    private long lastTime = System.currentTimeMillis();

    private boolean usedTimer = false;

    private float targetTimerSpeed = 1.0f;

    public FastUse() {
        super("FastUse", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(mode, customDelay, customSpeed, customTimer, dropTick, noMove);
        refreshVisibility();
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    @Override
    public void refreshVisibility() {
        boolean custom = mode.is("Custom");
        customDelay.setVisible(custom);
        customSpeed.setVisible(custom);
        customTimer.setVisible(custom);
        dropTick.setVisible(mode.is("Bug"));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        usedTimer = false;

        if (!isConsumingItem()) {
            lastTime = System.currentTimeMillis();
            return;
        }

        if (noMove.isEnabled()) {
            mc.thePlayer.motionX = 0.0d;
            mc.thePlayer.motionZ = 0.0d;
        }

        switch (mode.getValue()) {
            case "Instant": {
                for (int i = 0; i < 35; i++) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                }
                mc.playerController.onStoppedUsingItem(mc.thePlayer);
                break;
            }
            case "NCP": {
                if (mc.thePlayer.getItemInUseDuration() > 14) {
                    for (int i = 0; i < 20; i++) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                    }
                    mc.playerController.onStoppedUsingItem(mc.thePlayer);
                }
                break;
            }
            case "AAC": {
                targetTimerSpeed = 1.22f;
                usedTimer = true;
                break;
            }
            case "Custom": {
                targetTimerSpeed = customTimer.getValue().floatValue();
                usedTimer = true;

                if (System.currentTimeMillis() - lastTime < customDelay.getValue().longValue()) {
                    return;
                }

                int count = customSpeed.getValue().intValue();
                for (int i = 0; i < count; i++) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                }

                lastTime = System.currentTimeMillis();
                break;
            }
            case "Bug": {
                ItemStack stack = mc.thePlayer.getItemInUse();

                if (stack != null && stack.getItem() instanceof ItemFood
                        && stack.stackSize > 1
                        && mc.thePlayer.getItemInUseDuration() == dropTick.getValue().intValue()) {
                    mc.thePlayer.dropOneItem(false);
                }
                break;
            }
        }
    }

    @EventHandler
    public void onGameLoop(GameLoopEvent event) {
        if (usedTimer && mc.thePlayer != null && !isStandaloneTimerActive()) {
            event.timerSpeed = targetTimerSpeed;
        }
    }

    private static boolean isStandaloneTimerActive() {
        Timer timer = Main.moduleManager.getModuleByClass(Timer.class);
        return timer != null && timer.isToggle();
    }

    @Override
    public void onDisable() {
        usedTimer = false;
    }

    private boolean isConsumingItem() {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem()) {
            return false;
        }
        ItemStack stack = mc.thePlayer.getItemInUse();
        return stack != null && stack.getItem() != null
                && (stack.getItem() instanceof ItemFood
                || stack.getItem() instanceof ItemPotion
                || stack.getItem() instanceof ItemBucketMilk);
    }
}
