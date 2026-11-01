package dev.astra.module.impl.combat.killaura.attack;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import dev.astra.Main;
import dev.astra.event.events.attack.SlowDownEvent;
import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.util.RandomUtils;
import dev.astra.util.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Attack {
    private static final long WHIFF_COOLDOWN_MS = 450L;

    private static final int MAX_TRACKED_ENTRIES = 64;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura parent;

    private final Map<UUID, Long> nextAttackTimes = new HashMap<>();

    private long lastWhiffTime = 0L;

    public Attack(KillAura parent) {
        this.parent = parent;
    }

public void tryAttack(EntityLivingBase target, float yaw, float pitch) {
        boolean visible = parent.throughWalls.getValue() || mc.thePlayer.canEntityBeSeen(target);
        double border = target.getCollisionBorderSize();
        AxisAlignedBB hitBox = target.getEntityBoundingBox().expand(border, border, border);
        double reach = RotationUtils.getOriginReach(hitBox, yaw, pitch);
        boolean hitConnect = reach != Double.MAX_VALUE;
        double effectiveRange = visible ? parent.range.getValue() : Math.min(parent.range.getValue(), 3.0D);
        boolean inAttackRange = hitConnect && reach <= effectiveRange;
        boolean inWhiffRange = !inAttackRange && visible && mc.thePlayer.getDistanceToEntity(target) <= parent.swingRange.getValue();
        if (!inAttackRange && !inWhiffRange) {
            return;
        }
        if (inAttackRange && parent.rayCastCheck.isEnabled()) {
            double rayLen = Math.max(parent.range.getValue(), parent.swingRange.getValue()) + 1.0D;
            Entity pointed = RotationUtils.rayTraceEntity(yaw, pitch, rayLen,
                    parent.hBoxSize.getValue(), parent.vBoxSize.getValue());
            if (pointed != target) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        UUID targetId = target.getUniqueID();
        if (now < nextAttackTimes.getOrDefault(targetId, 0L)) {
            return;
        }
        if (inAttackRange) {
            boolean smartGate = parent.smartAttack.isEnabled() && mc.thePlayer.hurtTime == 0;
            if (smartGate && target.hurtTime != 0 && target.hurtTime != 10) {
                return;
            }
            if (parent.attackMode.is("Legit")) {
                attackLegit(yaw, pitch);
            } else if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);
            } else {
                mc.playerController.attackEntity(mc.thePlayer, target);
                mc.thePlayer.swingItem();
            }
            double randomCps = RandomUtils.nextDouble(parent.cps.getMinValue(), parent.cps.getMaxValue());
            long delayMs = (long) (1000.0D / Math.max(randomCps, 0.1D));
            SlowDownEvent eventSlowDown = new SlowDownEvent(target, (int) Math.max(1, delayMs / 50));
            Main.eventBus.post(eventSlowDown);
            if (eventSlowDown.isCancelled()) {
                nextAttackTimes.put(targetId, now);
            } else {
                nextAttackTimes.put(targetId, now + Math.max(delayMs - 15L, 0));
            }
            pruneCooldowns(now);
        } else {
            if (!tryWhiffBlockClick(yaw, pitch)) {
                mc.thePlayer.swingItem();
            }
            double randomCps = RandomUtils.nextDouble(parent.cps.getMinValue(), parent.cps.getMaxValue());
            long delayMs = (long) (1000.0D / Math.max(randomCps, 0.1D));
            SlowDownEvent eventSlowDown = new SlowDownEvent(target, (int) Math.max(1, delayMs / 50));
            Main.eventBus.post(eventSlowDown);
            if (eventSlowDown.isCancelled()) {
                nextAttackTimes.put(targetId, now);
            } else {
                nextAttackTimes.put(targetId, now + Math.max(delayMs - 15L, 0));
            }
            lastWhiffTime = now;
            pruneCooldowns(now);
        }
    }

    public void reset() {
        nextAttackTimes.clear();
        lastWhiffTime = 0L;
    }

    private void attackLegit(float yaw, float pitch) {
        MovingObjectPosition previous = mc.objectMouseOver;
        mc.objectMouseOver = RotationUtils.getMouseOverFromRotation(yaw, pitch);
        try {
            mc.clickMouse();
        } finally {
            mc.objectMouseOver = previous;
        }
    }

    private boolean tryWhiffBlockClick(float yaw, float pitch) {
        if (!parent.attackMode.is("Legit")) return false;
        MovingObjectPosition mop = RotationUtils.getMouseOverFromRotation(yaw, pitch);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        MovingObjectPosition previous = mc.objectMouseOver;
        mc.objectMouseOver = mop;
        try {
            mc.clickMouse();
        } finally {
            mc.objectMouseOver = previous;
        }
        return true;
    }

    private void pruneCooldowns(long now) {
        nextAttackTimes.values().removeIf(deadline -> deadline <= now);
        while (nextAttackTimes.size() >= MAX_TRACKED_ENTRIES) {
            UUID oldestId = null;
            long oldest = Long.MAX_VALUE;
            for (Map.Entry<UUID, Long> entry : nextAttackTimes.entrySet()) {
                if (entry.getValue() < oldest) {
                    oldest = entry.getValue();
                    oldestId = entry.getKey();
                }
            }
            if (oldestId == null) break;
            nextAttackTimes.remove(oldestId);
        }
    }
}
