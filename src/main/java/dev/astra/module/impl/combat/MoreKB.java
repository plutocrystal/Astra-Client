package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.attack.AttackEntityEvent;
import dev.astra.event.events.player.PostUpdateEvent;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovingObjectPosition;

public class MoreKB extends Module {
    private static final int ADVANCED_MAX_HURT_TIME = 1;

    private final ModeValue mode = new ModeValue("Mode", "Packet", "Packet", "DoublePacket", "LessPacket", "Legit", "Advanced");

    private final BooleanValue onlyGround = new BooleanValue("OnlyGround", false);

    private final BooleanValue onlyHurtZero = new BooleanValue("OnlyHurtZero", false);

    private final BooleanValue intelligent = new BooleanValue("Intelligent", false);

    private boolean prevMoveForward = false;

    private boolean pendingRestore = false;

    private int pendingAge = 0;

    public MoreKB() {
        super("MoreKB", 0, Category.COMBAT);
        addValues(mode, onlyGround, onlyHurtZero, intelligent);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        prevMoveForward = false;
        pendingRestore = false;
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (!mode.is("Advanced")) return;
        if (mc.thePlayer == null) return;
        if (!mc.thePlayer.isSprinting() || !mc.thePlayer.onGround) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (target.hurtTime > (onlyHurtZero.isEnabled() ? 0 : ADVANCED_MAX_HURT_TIME)) return;
        prevMoveForward = mc.gameSettings.keyBindForward.pressed;
        mc.gameSettings.keyBindForward.pressed = false;
        pendingRestore = true;
        pendingAge = 0;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (pendingRestore && ++pendingAge >= 2) {
            restoreForwardKey();
        }
        if (mode.is("Advanced")) return;
        EntityLivingBase target = null;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            target = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }
        if (target == null) return;
        if (onlyGround.isEnabled() && !mc.thePlayer.onGround) return;
        if (intelligent.isEnabled()) {
            double x = mc.thePlayer.posX - target.posX;
            double z = mc.thePlayer.posZ - target.posZ;
            float calcYaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI - 90.0);
            float diff = Math.abs(net.minecraft.util.MathHelper.wrapAngleTo180_float(calcYaw - target.rotationYawHead));
            if (diff > 120.0f) return;
        }
        if (onlyHurtZero.isEnabled()) {
            if (target.hurtTime != 0) return;
        } else if (target.hurtTime != 10) {
            return;
        }
        switch (mode.getValue()) {
            case "Packet":
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                break;
            case "DoublePacket":
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                break;
            case "LessPacket":
                if (mc.thePlayer.isSprinting()) {
                    mc.thePlayer.setSprinting(false);
                }
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                break;
            case "Legit":
                mc.thePlayer.sprintingTicksLeft = 0;
                mc.thePlayer.setSprinting(false);
                mc.thePlayer.setSprinting(true);
                break;
        }
    }

    @EventHandler
    public void onPostUpdate(PostUpdateEvent event) {
        if (pendingRestore) {
            restoreForwardKey();
        }
    }

    private void restoreForwardKey() {
        if (mc.thePlayer == null) return;
        mc.gameSettings.keyBindForward.pressed = prevMoveForward;
        pendingRestore = false;
        pendingAge = 0;
    }
}
