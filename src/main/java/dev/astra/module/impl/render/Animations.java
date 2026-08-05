package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventRenderItem;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Animations extends Module {

    public ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "1.7", "Sigma", "Stab", "Exhibition");
    public NumberValue swingSpeed = new NumberValue("Swing Speed", 0.0, -500.0, 500.0, 1.0);

    public NumberValue x = new NumberValue("X", 0.0, -2.0, 2.0, 0.05);
    public NumberValue y = new NumberValue("Y", 0.0, -2.0, 2.0, 0.05);
    public NumberValue z = new NumberValue("Z", 0.0, -2.0, 2.0, 0.05);

    public NumberValue rotX = new NumberValue("Rotation X", 0.0, -180.0, 180.0, 1.0);
    public NumberValue rotY = new NumberValue("Rotation Y", 0.0, -180.0, 180.0, 1.0);
    public NumberValue rotZ = new NumberValue("Rotation Z", 0.0, -180.0, 180.0, 1.0);

    public Animations() {
        super("Animations", Keyboard.KEY_NONE, Category.RENDER);
        addValues(mode, swingSpeed, x, y, z, rotX, rotY, rotZ);
    }

    @EventHandler
    public void onRenderItem(EventRenderItem event) {
        float f = event.getEquipProgress();
        float f1 = event.getSwingProgress();
        float convertedProgress = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);

        switch (mode.getValue()) {
            case "1.7":
                mc.getItemRenderer().transformFirstPersonItem(f / 2.0F, f1);
                mc.getItemRenderer().doBlockTransformations();
                break;

            case "Sigma":
                mc.getItemRenderer().transformFirstPersonItem(f, 0.0F);
                float y = -convertedProgress * 2.0F;
                GlStateManager.translate(0.0F, y / 10.0F + 0.1F, 0.0F);
                GlStateManager.rotate(y * 10.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(250.0F, 0.2F, 1.0F, -0.6F);
                GlStateManager.rotate(-10.0F, 1.0F, 0.5F, 1.0F);
                GlStateManager.rotate(-y * 20.0F, 1.0F, 0.5F, 1.0F);
                break;

            case "Stab":
                float spin = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);
                GlStateManager.translate(0.6F, 0.3F, -0.6F + -spin * 0.7F);
                GlStateManager.rotate(6090.0F, 0.0F, 0.0F, 0.1F);
                GlStateManager.rotate(6085.0F, 0.0F, 0.1F, 0.0F);
                GlStateManager.rotate(6110.0F, 0.1F, 0.0F, 0.0F);
                mc.getItemRenderer().transformFirstPersonItem(0.0F, 0.0F);
                mc.getItemRenderer().doBlockTransformations();
                break;

            case "Exhibition":
                mc.getItemRenderer().transformFirstPersonItem(f / 2.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.3F, -0.0F);
                GlStateManager.rotate(-convertedProgress * 31.0F, 1.0F, 0.0F, 2.0F);
                GlStateManager.rotate(-convertedProgress * 33.0F, 1.5F, convertedProgress / 1.1F, 0.0F);
                mc.getItemRenderer().doBlockTransformations();
                break;

            case "Vanilla":
            default:
                mc.getItemRenderer().transformFirstPersonItem(f, 0.0F);
                mc.getItemRenderer().doBlockTransformations();
                break;
        }
    }
}