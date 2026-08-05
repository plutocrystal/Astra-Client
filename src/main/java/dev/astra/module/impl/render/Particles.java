package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventAttackEntity;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import org.lwjgl.input.Keyboard;

public class Particles extends Module {

    public static Particles INSTANCE;

    public final NumberValue crit = new NumberValue("Crit", 1.0, 0.0, 20.0, 1.0);
    public final NumberValue sharpness = new NumberValue("Sharpness", 1.0, 0.0, 20.0, 1.0);

    public Particles() {
        super("Particles", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        addValues(crit, sharpness);
    }

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Entity target = event.getTarget();
        if (target != null) {
            int critCount = (int) Math.round(this.crit.getValue());
            int sharpnessCount = (int) Math.round(this.sharpness.getValue());

            for (int i = 0; i < critCount; ++i) {
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT);
            }
            
            for (int i = 0; i < sharpnessCount; ++i) {
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT_MAGIC);
            }
        }
    }
}