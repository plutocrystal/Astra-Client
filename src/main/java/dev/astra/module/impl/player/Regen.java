package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.Potion;

public class Regen extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "OldSpartan", "NewSpartan", "AAC4NoFire", "1.17");
    private final NumberValue health = new NumberValue("Health", 18, 0, 20, 1);
    private final NumberValue food = new NumberValue("Food", 18, 0, 20, 1);
    private final NumberValue speed = new NumberValue("Speed", 100, 1, 100, 1);
    private final BooleanValue noAir = new BooleanValue("NoAir", false);
    private final BooleanValue potionEffect = new BooleanValue("PotionEffect", false);

    public Regen() {
        super("Regen", 0, Category.PLAYER);
        addValues(mode, health, food, speed, noAir, potionEffect);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if ((!noAir.isEnabled() || mc.thePlayer.onGround)
                && !mc.thePlayer.capabilities.isCreativeMode
                && mc.thePlayer.getFoodStats().getFoodLevel() > food.getValue()
                && mc.thePlayer.isEntityAlive()
                && mc.thePlayer.getHealth() < health.getValue()) {

            if (potionEffect.isEnabled() && !mc.thePlayer.isPotionActive(Potion.regeneration)) {
                return;
            }

            switch (mode.getValue()) {
                case "Vanilla": {
                    int count = speed.getValue().intValue();
                    for (int i = 0; i < count; i++) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                    }
                    break;
                }

                case "AAC4NoFire": {
                    if (mc.thePlayer.isBurning() && mc.thePlayer.ticksExisted % 10 == 0) {
                        for (int i = 0; i < 35; i++) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
                        }
                    }
                    break;
                }

                case "NewSpartan": {
                    if (mc.thePlayer.ticksExisted % 5 == 0) {
                        for (int i = 0; i < 10; i++) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
                        }
                    } else {
                        if (isMoving()) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                        }
                    }
                    break;
                }

                case "OldSpartan": {
                    if (isMoving() || !mc.thePlayer.onGround) {
                        return;
                    }

                    for (int i = 0; i < 9; i++) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                    }
                    break;
                }

                case "1.17": {
                    for (int i = 0; i < 40; i++) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ,
                                mc.thePlayer.rotationYaw,
                                mc.thePlayer.rotationPitch,
                                mc.thePlayer.onGround
                        ));
                    }
                    break;
                }
            }
        }
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0);
    }
}
