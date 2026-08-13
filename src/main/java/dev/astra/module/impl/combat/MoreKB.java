package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovingObjectPosition;

public class MoreKB extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Packet", "Packet", "DoublePacket", "LessPacket", "Legit");
    private final BooleanValue onlyGround = new BooleanValue("OnlyGround", false);
    private final BooleanValue intelligent = new BooleanValue("Intelligent", false);

    public MoreKB() {
        super("MoreKB", 0, Category.COMBAT);
        addValues(mode, onlyGround, intelligent);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

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

        if (target.hurtTime != 10) return;

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
}
