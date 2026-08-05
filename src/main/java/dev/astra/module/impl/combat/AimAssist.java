package dev.astra.module.impl.combat;

import dev.astra.event.EventHandler;
import dev.astra.event.events.Render3DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
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

public class AimAssist extends Module {

    public final NumberValue range = new NumberValue("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberValue speed = new NumberValue("Speed", 5.0, 1.0, 10.0, 0.5);
    public final NumberValue fov = new NumberValue("FOV", 90.0, 15.0, 180.0, 5.0);
    public final BooleanValue throughWalls = new BooleanValue("ThroughWalls", false);
    public final BooleanValue smoothPitch = new BooleanValue("SmoothPitch", true);

    public AimAssist() {
        super("AimAssist", Keyboard.KEY_NONE, Category.COMBAT);
        addValues(range, speed, fov, throughWalls, smoothPitch);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        EntityLivingBase target = getTarget();
        if (target == null) return;

        double diffX = target.posX - mc.thePlayer.posX;
        double diffY = target.posY + (target.height / 2.0) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;

        double smoothness = 1.0 - (speed.getValue() / 10.0);
        if (smoothness < 0) smoothness = 0;

        mc.thePlayer.rotationYaw += yawDiff * (1.0 - smoothness);
        if (smoothPitch.getValue()) {
            mc.thePlayer.rotationPitch += pitchDiff * (1.0 - smoothness);
        }
    }

    private EntityLivingBase getTarget() {
        List<EntityLivingBase> entities = mc.theWorld.loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase && e != mc.thePlayer)
                .map(e -> (EntityLivingBase) e)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                .collect(Collectors.toList());

        return entities.isEmpty() ? null : entities.get(0);
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

        double diffX = e.posX - mc.thePlayer.posX;
        double diffZ = e.posZ - mc.thePlayer.posZ;
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        if (Math.abs(yawDiff) > fov.getValue() / 2.0f) return false;

        if (!throughWalls.getValue() && !mc.thePlayer.canEntityBeSeen(e)) return false;

        return true;
    }
}