package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.module.impl.combat.Target;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

public class Chams extends Module {

    public static Chams INSTANCE;

    public Chams() {
        super("Chams", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
    }

    public boolean isValid(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) return false;
        if (entity == mc.thePlayer) return false;

        EntityLivingBase e = (EntityLivingBase) entity;
        boolean isDead = e.isDead || e.getHealth() <= 0 || e.deathTime > 0;
        if (isDead && !Target.dead.getValue()) return false;

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

        return true;
    }
}