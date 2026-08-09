package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.utils.rotation.RotationManager;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KillAura extends Module {

    public final NumberValue range = new NumberValue("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberValue fov = new NumberValue("FOV", 180.0, 15.0, 360.0, 5.0);
    public final BooleanValue throughWalls = new BooleanValue("ThroughWalls", false);
    public final ModeValue moveFix = new ModeValue("MoveFix", "STRICT", "NONE", "STRICT", "SILENT");

    public KillAura() {
        super("KillAura", Keyboard.KEY_R, Category.COMBAT);
        addValues(range, fov, throughWalls, moveFix);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        List<EntityLivingBase> targets = mc.theWorld.loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase && e != mc.thePlayer)
                .map(e -> (EntityLivingBase) e)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            RotationManager.clearRotation();
            return;
        }

        EntityLivingBase target = targets.get(0);

        double diffX = target.posX - mc.thePlayer.posX;
        double diffY = target.posY + (target.height / 2.0) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        targetYaw = MathHelper.wrapAngleTo180_float(targetYaw);
        if (targetPitch > 90.0F) targetPitch = 90.0F;
        if (targetPitch < -90.0F) targetPitch = -90.0F;

        RotationManager.MoveFix fixMode;
        switch (moveFix.getValue()) {
            case "STRICT":
                fixMode = RotationManager.MoveFix.STRICT;
                break;
            case "SILENT":
                fixMode = RotationManager.MoveFix.SILENT;
                break;
            default:
                fixMode = RotationManager.MoveFix.NONE;
                break;
        }

        RotationManager.setRotation(targetYaw, targetPitch, fixMode);

        if (mc.thePlayer.getDistanceToEntity(target) <= range.getValue()) {
            if (mc.leftClickCounter <= 0) {
                mc.clickMouse();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        RotationManager.clearRotation();
    }

    private boolean isValidTarget(EntityLivingBase e) {
        boolean isDead = e.isDead || e.getHealth() <= 0 || e.deathTime > 0;
        if (isDead && !Target.dead.getValue()) return false;
        
        if (!isDead && Target.dead.getValue() && !Target.players.getValue() && !Target.mobs.getValue() && !Target.animals.getValue()) {
            return false;
        }

        if (mc.thePlayer.getDistanceToEntity(e) > range.getValue()) return false;

        if (e instanceof EntityPlayer) {
            if (!Target.players.getValue()) return false;
        } else if (e instanceof EntityMob) {
            if (!Target.mobs.getValue()) return false;
        } else if (e instanceof EntityAnimal) {
            if (!Target.animals.getValue()) return false;
        } else {
            return false;
        }

        if (e.isInvisible() && !Target.invisible.getValue()) return false;

        if (!throughWalls.getValue() && !mc.thePlayer.canEntityBeSeen(e)) return false;

        if (fov.getValue() < 360.0) {
            double diffX = e.posX - mc.thePlayer.posX;
            double diffZ = e.posZ - mc.thePlayer.posZ;
            float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
            float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
            if (Math.abs(yawDiff) > fov.getValue() / 2.0f) return false;
        }

        return true;
    }
}