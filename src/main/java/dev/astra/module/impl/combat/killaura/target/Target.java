package dev.astra.module.impl.combat.killaura.target;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.module.impl.misc.Teams;
import dev.astra.util.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Target {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura parent;

    private EntityLivingBase currentTarget;

    private long lastSwitchTime = 0L;

    public Target(KillAura parent) {
        this.parent = parent;
    }

    public List<EntityLivingBase> findTargets() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase && e != mc.thePlayer)
                .map(e -> (EntityLivingBase) e)
                .filter(this::isValidTarget)
                .sorted(priorityComparator())
                .collect(Collectors.toList());
    }

    private Comparator<EntityLivingBase> priorityComparator() {
        switch (parent.priority.getValue()) {
            case "Health":
                return Comparator.comparingDouble(EntityLivingBase::getHealth);
            case "Angle":
                return Comparator.comparingDouble(this::angleTo);
            case "Armor":
                return Comparator.comparingInt(EntityLivingBase::getTotalArmorValue);
            case "Distance":
            default:
                return Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e));
        }
    }

    private float angleTo(EntityLivingBase e) {
        double diffX = e.posX - mc.thePlayer.posX;
        double diffZ = e.posZ - mc.thePlayer.posZ;
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        return Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw));
    }

    public EntityLivingBase selectTarget(List<EntityLivingBase> targets) {
        if (targets.isEmpty()) {
            reset();
            return null;
        }
        if (parent.targetMode.is("Single")) {
            if (currentTarget != null && targets.contains(currentTarget)
                    && parent.isWithinAimRange(currentTarget)) {
                return currentTarget;
            }
            currentTarget = targets.get(0);
            return currentTarget;
        }
        long now = System.currentTimeMillis();
        long delay = parent.switchDelay.getValue().longValue();
        if (currentTarget != null && targets.contains(currentTarget) && delay > 0 && now - lastSwitchTime < delay) {
            return currentTarget;
        }
        EntityLivingBase next = targets.stream()
                .filter(t -> t != currentTarget)
                .findFirst()
                .orElse(targets.get(0));
        if (next != currentTarget) {
            lastSwitchTime = now;
        }
        currentTarget = next;
        return next;
    }

    public void reset() {
        currentTarget = null;
        lastSwitchTime = 0L;
    }

    private boolean isValidTarget(EntityLivingBase e) {
        boolean isDead = e.isDead || e.getHealth() <= 0 || e.deathTime > 0;
        if (isDead && !dev.astra.module.impl.combat.Target.dead.getValue()) return false;
        if (!isDead && dev.astra.module.impl.combat.Target.dead.getValue()
                && !dev.astra.module.impl.combat.Target.players.getValue()
                && !dev.astra.module.impl.combat.Target.mobs.getValue()
                && !dev.astra.module.impl.combat.Target.animals.getValue()) {
            return false;
        }
        if (Teams.isInYourTeam(e)) return false;
        if (mc.thePlayer.getDistanceToEntity(e) > parent.getTargetScanRange()) return false;
        if (e instanceof EntityPlayer) {
            if (!dev.astra.module.impl.combat.Target.players.getValue()) return false;
            if (!dev.astra.module.impl.combat.Target.friends.getValue()
                    && FriendManager.isFriend(e.getName())) return false;
        } else if (e instanceof EntityMob) {
            if (!dev.astra.module.impl.combat.Target.mobs.getValue()) return false;
        } else if (e instanceof EntityAnimal) {
            if (!dev.astra.module.impl.combat.Target.animals.getValue()) return false;
        } else {
            return false;
        }
        if (e.isInvisible() && !dev.astra.module.impl.combat.Target.invisible.getValue()) return false;
        if (!parent.throughWalls.getValue() && !mc.thePlayer.canEntityBeSeen(e)
                && mc.thePlayer.getDistanceToEntity(e) > 3.0D) return false;
        if (parent.fov.getValue() < 360.0) {
            double diffX = e.posX - mc.thePlayer.posX;
            double diffZ = e.posZ - mc.thePlayer.posZ;
            float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
            float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
            if (Math.abs(yawDiff) > parent.fov.getValue() / 2.0f) return false;
        }
        return true;
    }
}
