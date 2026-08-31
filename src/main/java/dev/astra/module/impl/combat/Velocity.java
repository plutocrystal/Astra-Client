package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.packet.PacketReceiveEvent;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import java.lang.reflect.Field;
import java.util.Random;

public class Velocity extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Control", "Control", "Jump");

    private final NumberValue horizontal = new NumberValue("Horizontal", 0.0, 0.0, 1.0, 0.01);

    private final NumberValue vertical = new NumberValue("Vertical", 0.0, 0.0, 1.0, 0.01);

    private final NumberValue chance = new NumberValue("Chance", 100.0, 0.0, 100.0, 1.0);

    private boolean jumpFlag = false;

    private final Random random = new Random();

    public Velocity() {
        super("Velocity", 0, Category.COMBAT);
        addValues(mode, horizontal, vertical, chance);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        jumpFlag = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        jumpFlag = false;
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;
            if (mode.getValue().equals("Control")) {
                if (random.nextInt(100) >= chance.getValue()) return;
                scaleIntVelocity(packet, horizontal.getValue(), vertical.getValue());
            } else if (mode.getValue().equals("Jump")) {
                if (packet.getMotionY() > 0 && random.nextInt(100) < chance.getValue()) {
                    jumpFlag = true;
                }
            }
        } else if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            if (mode.getValue().equals("Control")) {
                if (random.nextInt(100) >= chance.getValue()) return;
                scaleFloatVelocity(packet, horizontal.getValue(), vertical.getValue());
            }
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!mode.getValue().equals("Jump")) return;
        if (jumpFlag) {
            jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting()
                    && !mc.thePlayer.isPotionActive(Potion.jump)
                    && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    private static void scaleIntVelocity(S12PacketEntityVelocity packet, double h, double v) {
        try {
            int entityId = packet.getEntityID();
            int mx = packet.getMotionX();
            int my = packet.getMotionY();
            int mz = packet.getMotionZ();
            for (Field f : S12PacketEntityVelocity.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    int val = f.getInt(packet);
                    if (val == entityId) continue;
                    if (val == mx) f.setInt(packet, (int) (mx * h));
                    else if (val == my) f.setInt(packet, (int) (my * v));
                    else if (val == mz) f.setInt(packet, (int) (mz * h));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void scaleFloatVelocity(S27PacketExplosion packet, double h, double v) {
        try {
            float strength = packet.getStrength();
            float mx = packet.func_149149_c();
            float my = packet.func_149144_d();
            float mz = packet.func_149147_e();
            for (Field f : S27PacketExplosion.class.getDeclaredFields()) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    float val = f.getFloat(packet);
                    if (val == strength) continue;
                    if (val == mx) f.setFloat(packet, (float) (mx * h));
                    else if (val == my) f.setFloat(packet, (float) (my * v));
                    else if (val == mz) f.setFloat(packet, (float) (mz * h));
                }
            }
        } catch (Exception ignored) {
        }
    }
}
