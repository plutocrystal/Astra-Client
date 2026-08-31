package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.player.PostUpdateEvent;
import dev.astra.event.events.render.RenderItemEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.GroupValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Animations extends Module {
    public ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "1.7", "Sigma", "Stab", "Exhibition");

    public BooleanValue blockAnything = new BooleanValue("BlockAnything", false);

    public BooleanValue leftHand = new BooleanValue("LeftHand", false);

    public ModeValue swingMode = new ModeValue("Swing", "None", "None", "1.9+", "Smooth", "Punch", "Shove");

    private int swingCounter = 0;

    public NumberValue heldX = new NumberValue("X", 0.0, -2.0, 2.0, 0.05);

    public NumberValue heldY = new NumberValue("Y", 0.0, -2.0, 2.0, 0.05);

    public NumberValue heldZ = new NumberValue("Z", 0.0, -2.0, 2.0, 0.05);

    public NumberValue heldSize = new NumberValue("Size", 1.0, 0.5, 2.0, 0.05);

    public NumberValue heldRotX = new NumberValue("Rotation X", 0.0, -180.0, 180.0, 1.0);

    public NumberValue heldRotY = new NumberValue("Rotation Y", 0.0, -180.0, 180.0, 1.0);

    public NumberValue heldRotZ = new NumberValue("Rotation Z", 0.0, -180.0, 180.0, 1.0);

    public NumberValue heldSpeed = new NumberValue("Speed", 0.0, -500.0, 500.0, 1.0);

    public NumberValue blockX = new NumberValue("X", 0.0, -2.0, 2.0, 0.05);

    public NumberValue blockY = new NumberValue("Y", 0.0, -2.0, 2.0, 0.05);

    public NumberValue blockZ = new NumberValue("Z", 0.0, -2.0, 2.0, 0.05);

    public NumberValue blockSize = new NumberValue("Size", 1.0, 0.5, 2.0, 0.05);

    public NumberValue blockRotX = new NumberValue("Rotation X", 0.0, -180.0, 180.0, 1.0);

    public NumberValue blockRotY = new NumberValue("Rotation Y", 0.0, -180.0, 180.0, 1.0);

    public NumberValue blockRotZ = new NumberValue("Rotation Z", 0.0, -180.0, 180.0, 1.0);

    public NumberValue blockSpeed = new NumberValue("Speed", 0.0, -500.0, 500.0, 1.0);

    public GroupValue held = new GroupValue("Held", heldX, heldY, heldZ, heldSize,
            heldRotX, heldRotY, heldRotZ, heldSpeed);

    public GroupValue blocking = new GroupValue("Blocking", blockX, blockY, blockZ, blockSize,
            blockRotX, blockRotY, blockRotZ, blockSpeed);

    public Animations() {
        super("Animations", Keyboard.KEY_NONE, Category.RENDER);
        addValues(mode, blockAnything, leftHand, swingMode, held, blocking);
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    @EventHandler
    public void onPostUpdate(PostUpdateEvent event) {
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.swingProgressInt == 1) {
            swingCounter = 9;
        } else {
            swingCounter = Math.max(0, swingCounter - 1);
        }
    }

    public boolean applySwing(float equipProgress, float swingProgress) {
        switch (swingMode.getValue()) {
            case "1.9+": {
                applySwingOffset(swingProgress);
                mc.getItemRenderer().transformFirstPersonItem(equipProgress, swingProgress);
                float partialTicks = mc.getTimer() != null ? mc.getTimer().renderPartialTicks : 0.0f;
                GlStateManager.translate(0.0F, -((swingCounter - 1) - (swingCounter == 0 ? 0 : partialTicks)) / 5.0f, 0.0F);
                return true;
            }
            case "Smooth": {
                mc.getItemRenderer().transformFirstPersonItem(equipProgress, swingProgress);
                applySwingOffset(equipProgress);
                return true;
            }
            case "Punch": {
                mc.getItemRenderer().transformFirstPersonItem(equipProgress, swingProgress);
                applySwingOffset(swingProgress);
                return true;
            }
            case "Shove": {
                mc.getItemRenderer().transformFirstPersonItem(equipProgress, equipProgress);
                applySwingOffset(swingProgress);
                return true;
            }
            default:
                return false;
        }
    }

    private void applySwingOffset(float swingProgress) {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(f, f1, f2);
    }

    public boolean isBlockState() {
        if (!isToggle() || mc.thePlayer == null) return false;
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null) return false;
        boolean blockAnythingActive = blockAnything.getValue()
                && mc.thePlayer.getItemInUseCount() <= 0
                && mc.currentScreen == null
                && mc.gameSettings.keyBindUseItem.isKeyDown();
        EnumAction action = stack.getItemUseAction();
        if (blockAnythingActive && action != EnumAction.BLOCK) {
            action = EnumAction.BLOCK;
        }
        boolean usingOrFake = mc.thePlayer.getItemInUseCount() > 0
                || KillAura.shouldFakeBlock(stack)
                || blockAnythingActive;
        return usingOrFake && action == EnumAction.BLOCK;
    }

    @EventHandler
    public void onRenderItem(RenderItemEvent event) {
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
