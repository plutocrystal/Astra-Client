package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.attack.AttackEntityEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;
import net.minecraft.block.Block;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

public class Particles extends Module {
    public static Particles INSTANCE;

    public final NumberValue crit = new NumberValue("Crit", 1.0, 0.0, 20.0, 1.0);

    public final NumberValue sharpness = new NumberValue("Sharpness", 1.0, 0.0, 20.0, 1.0);

    public final NumberValue blood = new NumberValue("Blood", 0.0, 0.0, 20.0, 1.0);

    public final NumberValue lightningCount = new NumberValue("LightningCount", 0.0, 0.0, 20.0, 1.0);

    public Particles() {
        super("Particles", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        addValues(crit, sharpness, blood, lightningCount);
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        Entity target = event.getTarget();
        if (target != null) {
            int critCount = (int) Math.round(this.crit.getValue());
            int sharpnessCount = (int) Math.round(this.sharpness.getValue());
            int bloodCount = (int) Math.round(this.blood.getValue());
            int bolts = (int) Math.round(this.lightningCount.getValue());
            for (int i = 0; i < critCount; ++i) {
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT);
            }
            for (int i = 0; i < sharpnessCount; ++i) {
                mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT_MAGIC);
            }
            if (bloodCount > 0) {
                int redstoneStateId = Block.getStateId(Blocks.redstone_block.getDefaultState());
                for (int i = 0; i < bloodCount; ++i) {
                    double ox = (Math.random() - 0.5D) * target.width;
                    double oy = Math.random() * target.height;
                    double oz = (Math.random() - 0.5D) * target.width;
                    mc.effectRenderer.spawnEffectParticle(
                            EnumParticleTypes.BLOCK_CRACK.getParticleID(),
                            target.posX + ox,
                            target.posY + oy,
                            target.posZ + oz,
                            0.0D, 0.0D, 0.0D,
                            redstoneStateId);
                }
            }
            if (bolts > 0 && mc.theWorld.isRemote) {
                for (int i = 0; i < bolts; ++i) {
                    mc.theWorld.addWeatherEffect(new EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ));
                }
                float thunderPitch = 0.8F + (float) Math.random() * 0.2F;
                float explodePitch = 0.5F + (float) Math.random() * 0.2F;
                mc.getSoundHandler().playSound(new PositionedSoundRecord(
                        new ResourceLocation("ambient.weather.thunder"), 10000.0F, thunderPitch,
                        (float) target.posX, (float) target.posY, (float) target.posZ));
                mc.getSoundHandler().playSound(new PositionedSoundRecord(
                        new ResourceLocation("random.explode"), 2.0F, explodePitch,
                        (float) target.posX, (float) target.posY, (float) target.posZ));
            }
        }
    }
}
