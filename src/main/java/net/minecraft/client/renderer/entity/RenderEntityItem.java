package net.minecraft.client.renderer.entity;

import dev.astra.module.impl.render.ItemPhysics;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.util.Random;

public class RenderEntityItem extends Render<EntityItem> {
    private final RenderItem itemRenderer;
    private Random field_177079_e;

    private static final double CONSTANT_SCALE_DISTANCE = 20.0;
    private static final double DISTANCE_SCALE_FACTOR = 0.0075;
    private static final double LABEL_HEIGHT = 0.25;

    public RenderEntityItem(final RenderManager renderManagerIn, final RenderItem p_i46167_2_) {
        super(renderManagerIn);
        this.field_177079_e = new Random();
        this.itemRenderer = p_i46167_2_;
        this.shadowSize = 0.15f;
        this.shadowOpaque = 0.75f;
    }

    @Override
    public void doRender(final EntityItem entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks) {
        final ItemPhysics itemPhysics = ItemPhysics.INSTANCE;
        final ItemStack stack = entity.getEntityItem();
        if (itemPhysics == null || !itemPhysics.isToggle() || stack == null || stack.getItem() == null) {
            this.doVanillaRender(entity, x, y, z, entityYaw, partialTicks);
            return;
        }

        this.field_177079_e.setSeed(187L);
        boolean blurred = false;
        if (this.bindEntityTexture(entity)) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).setBlurMipmap(false, false);
            blurred = true;
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();

        IBakedModel model = this.itemRenderer.getItemModelMesher().getItemModel(stack);
        final Vector3f groundScale = model.getItemCameraTransforms().ground.scale;
        final int count = this.physicsLayout(itemPhysics, entity, x, y, z, partialTicks, model);

        for (int j = 0; j < count; ++j) {
            if (model.isGui3d()) {
                GlStateManager.pushMatrix();
                if (j > 0) {
                    final float jitter = itemPhysics.is17Mode() ? 0.2f : 0.15f;
                    GlStateManager.translate(
                            this.shouldSpreadItems() ? this.jitter(jitter) : 0.0f,
                            this.shouldSpreadItems() ? this.jitter(jitter) : 0.0f,
                            this.jitter(jitter)
                    );
                }
                final float sc = 0.5f * itemPhysics.getScale();
                GlStateManager.scale(sc, sc, sc);
                model.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                this.itemRenderer.renderItem(stack, model);
                GlStateManager.popMatrix();
            } else {
                GlStateManager.pushMatrix();
                if (itemPhysics.is17Mode() && j > 0) {
                    GlStateManager.translate(
                            this.shouldSpreadItems() ? this.jitter(0.15f) : 0.0f,
                            this.shouldSpreadItems() ? this.jitter(0.15f) : 0.0f,
                            this.jitter(0.15f)
                    );
                }
                if (itemPhysics.is17Mode() && !this.isFancy()) {
                    GlStateManager.rotate(180.0f - this.renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
                }
                final float sc = itemPhysics.getScale();
                GlStateManager.scale(sc, sc, sc);
                model.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                this.itemRenderer.renderItem(stack, model);
                GlStateManager.popMatrix();
                final float step = itemPhysics.is17Mode()
                        ? (this.isFancy() ? 0.0421875f : 0.0f)
                        : 0.046875f * groundScale.z;
                GlStateManager.translate(0.0f * groundScale.x, 0.0f * groundScale.y, step);
            }
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        this.bindEntityTexture(entity);
        if (blurred) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).restoreLastBlurMipmap();
        }
        this.renderItemOverlayPhysics(itemPhysics, entity, x, y, z, partialTicks);
        this.renderItemName(itemPhysics, entity, x, y, z, partialTicks);
    }

    private void doVanillaRender(final EntityItem entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks) {
        final ItemStack itemstack = entity.getEntityItem();
        this.field_177079_e.setSeed(187L);
        boolean flag = false;
        if (this.bindEntityTexture(entity)) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).setBlurMipmap(false, false);
            flag = true;
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        final IBakedModel ibakedmodel = this.itemRenderer.getItemModelMesher().getItemModel(itemstack);
        for (int i = this.func_177077_a(entity, x, y, z, partialTicks, ibakedmodel), j = 0; j < i; ++j) {
            if (ibakedmodel.isGui3d()) {
                GlStateManager.pushMatrix();
                if (j > 0) {
                    final float f = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    final float f2 = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    final float f3 = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    GlStateManager.translate(f, f2, f3);
                }
                GlStateManager.scale(0.5f, 0.5f, 0.5f);
                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                this.itemRenderer.renderItem(itemstack, ibakedmodel);
                GlStateManager.popMatrix();
            } else {
                GlStateManager.pushMatrix();
                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                this.itemRenderer.renderItem(itemstack, ibakedmodel);
                GlStateManager.popMatrix();
                final float f4 = ibakedmodel.getItemCameraTransforms().ground.scale.x;
                final float f5 = ibakedmodel.getItemCameraTransforms().ground.scale.y;
                final float f6 = ibakedmodel.getItemCameraTransforms().ground.scale.z;
                GlStateManager.translate(0.0f * f4, 0.0f * f5, 0.046875f * f6);
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        this.bindEntityTexture(entity);
        if (flag) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).restoreLastBlurMipmap();
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private int physicsLayout(final ItemPhysics itemPhysics, final EntityItem entity, final double x, final double y, final double z, final float partialTicks, final IBakedModel model) {
        final ItemStack stack = entity.getEntityItem();
        if (stack.getItem() == null) {
            return 0;
        }
        final boolean is3D = model.isGui3d();
        final boolean fancy = this.isFancy();
        final int count = itemPhysics.isCountEnabled() ? 1
                : (itemPhysics.is17Mode()
                ? (fancy && !is3D ? ItemPhysics.get17FlatCount(stack) : ItemPhysics.get17BlockCount(stack))
                : this.func_177078_a(stack));
        final float bob = itemPhysics.isPhysicsMode() ? 0.0f
                : (this.shouldBob()
                ? MathHelper.sin((entity.getAge() + partialTicks) / 10.0f + entity.hoverStart) * 0.1f + 0.1f
                : 0.0f);
        final float modelScale = model.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
        if (itemPhysics.is17Mode() && !is3D) {
            GlStateManager.translate((float) x, (float) y + bob + 0.05f * modelScale + 0.2f, (float) z);
        } else if (itemPhysics.isPhysicsMode()) {
            GlStateManager.translate((float) x, (float) y + bob + 0.05f * modelScale, (float) z);
        } else {
            GlStateManager.translate((float) x, (float) y + bob + 0.25f * modelScale, (float) z);
        }
        if (is3D || this.renderManager.options != null) {
            final float spin = ((entity.getAge() + partialTicks) / 20.0f + entity.hoverStart) * 57.295776f;
            if (!(itemPhysics.is17Mode() && !is3D && !fancy)) {
                if (itemPhysics.isPhysicsMode()) {
                    if (entity.onGround) {
                        GL11.glRotatef(entity.rotationYaw, 0.0f, 1.0f, 0.0f);
                        GL11.glRotatef(entity.rotationPitch + 90.0f, 1.0f, 0.0f, 0.0f);
                    } else {
                        for (int a = 0; a < 10; ++a) {
                            GL11.glRotatef(spin, 0.7f, 0.7f, 0.0f);
                        }
                    }
                } else {
                    GlStateManager.rotate(spin, 0.0f, 1.0f, 0.0f);
                }
            }
        }
        if (!is3D) {
            final float step = itemPhysics.is17Mode()
                    ? (fancy ? -0.02109375f * count + 0.0421875f : 0.0f)
                    : -0.046875f * (count - 1) * 0.5f;
            GlStateManager.translate(-0.0f * (count - 1) * 0.5f, -0.0f * (count - 1) * 0.5f, step);
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        return count;
    }

    private int func_177077_a(final EntityItem itemIn, final double p_177077_2_, final double p_177077_4_, final double p_177077_6_, final float p_177077_8_, final IBakedModel p_177077_9_) {
        final ItemStack itemstack = itemIn.getEntityItem();
        if (itemstack.getItem() == null) {
            return 0;
        }
        final boolean flag = p_177077_9_.isGui3d();
        final int i = this.func_177078_a(itemstack);
        final float f2 = MathHelper.sin((itemIn.getAge() + p_177077_8_) / 10.0f + itemIn.hoverStart) * 0.1f + 0.1f;
        final float f3 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
        GlStateManager.translate((float) p_177077_2_, (float) p_177077_4_ + f2 + 0.25f * f3, (float) p_177077_6_);
        if (flag || this.renderManager.options != null) {
            final float f4 = ((itemIn.getAge() + p_177077_8_) / 20.0f + itemIn.hoverStart) * 57.295776f;
            GlStateManager.rotate(f4, 0.0f, 1.0f, 0.0f);
        }
        if (!flag) {
            final float f5 = -0.0f * (i - 1) * 0.5f;
            final float f6 = -0.0f * (i - 1) * 0.5f;
            final float f7 = -0.046875f * (i - 1) * 0.5f;
            GlStateManager.translate(f5, f6, f7);
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        return i;
    }

    private int func_177078_a(final ItemStack stack) {
        int i = 1;
        if (stack.stackSize > 48) {
            i = 5;
        } else if (stack.stackSize > 32) {
            i = 4;
        } else if (stack.stackSize > 16) {
            i = 3;
        } else if (stack.stackSize > 1) {
            i = 2;
        }
        return i;
    }

    private void renderItemOverlayPhysics(final ItemPhysics itemPhysics, final EntityItem entity, final double x, final double y, final double z, final float partialTicks) {
        final ItemStack stack = entity.getEntityItem();
        if (stack == null || stack.getItem() == null) {
            return;
        }
        final boolean drawCount = itemPhysics.isCountEnabled() && stack.stackSize > 1;
        final boolean drawDurability = itemPhysics.isDurabilityEnabled() && stack.isItemStackDamageable() && stack.getItemDamage() > 0;
        if (!drawCount && !drawDurability) {
            return;
        }
        final FontRenderer fontRenderer = this.getFontRendererFromRenderManager();
        if (fontRenderer == null) {
            return;
        }
        final float bob = MathHelper.sin((entity.getAge() + partialTicks) / 10.0f + entity.hoverStart) * 0.1f + 0.1f;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + bob + 0.45f, (float) z);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-0.02f, -0.02f, 0.02f);
        GlStateManager.translate(-8.0f, 0.0f, 0.0f);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        if (drawDurability) {
            final int width = (int) Math.round(13.0d - (double) stack.getItemDamage() * 13.0d / (double) stack.getMaxDamage());
            final int green = (int) Math.round(255.0d - (double) stack.getItemDamage() * 255.0d / (double) stack.getMaxDamage());
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            this.drawOverlayQuad(2, 13, 13, 2, 0, 0, 0, 255);
            this.drawOverlayQuad(2, 13, 12, 1, (255 - green) / 4, 64, 0, 255);
            this.drawOverlayQuad(2, 13, width, 1, 255 - green, green, 0, 255);
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
        }
        if (drawCount) {
            final String text = String.valueOf(stack.stackSize);
            GlStateManager.disableBlend();
            fontRenderer.drawStringWithShadow(text, 17.0f - (float) fontRenderer.getStringWidth(text), 9.0f, 16777215);
            GlStateManager.enableBlend();
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void renderItemName(final ItemPhysics itemPhysics, final EntityItem entity, final double x, final double y, final double z, final float partialTicks) {
        if (!itemPhysics.isItemNameEnabled()) {
            return;
        }
        final ItemStack stack = entity.getEntityItem();
        if (stack == null || stack.getItem() == null) {
            return;
        }
        final String name = stack.getDisplayName();
        if (name == null || name.isEmpty()) {
            return;
        }
        final FontRenderer fontRenderer = this.getFontRendererFromRenderManager();
        if (fontRenderer == null) {
            return;
        }
        final int width = fontRenderer.getStringWidth(name);
        final double lerpX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - this.renderManager.viewerPosX;
        final double lerpY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - this.renderManager.viewerPosY + LABEL_HEIGHT;
        final double lerpZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - this.renderManager.viewerPosZ;
        final double distance = this.renderManager.livingPlayer != null
                ? this.renderManager.livingPlayer.getDistanceToEntity(entity)
                : 0.0d;
        final double scale = this.getNameScale(distance);

        GlStateManager.pushMatrix();
        GlStateManager.translate(lerpX, lerpY, lerpZ);
        GlStateManager.rotate(this.renderManager.playerViewY * -1.0f, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-scale, -scale, 1.0d);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        fontRenderer.drawString(name, -width / 2.0f, -fontRenderer.FONT_HEIGHT, 0xFFFFFFFF, true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private double getNameScale(final double distance) {
        if (ItemPhysics.nameAutoScale.getValue()) {
            return Math.pow(Math.min(Math.max(distance, 6.0d), 128.0d), 0.75d) * DISTANCE_SCALE_FACTOR;
        }
        return Math.pow(CONSTANT_SCALE_DISTANCE, 0.75d) * DISTANCE_SCALE_FACTOR * ItemPhysics.nameSize.getValue().doubleValue();
    }

    private void drawOverlayQuad(final int x, final int y, final int width, final int height, final int red, final int green, final int blue, final int alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        final WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x, y, 0.0d).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x, y + height, 0.0d).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0d).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + width, y, 0.0d).color(red, green, blue, alpha).endVertex();
        Tessellator.getInstance().draw();
    }

    private float jitter(final float amount) {
        return (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * amount;
    }

    private boolean isFancy() {
        return this.renderManager.options == null || this.renderManager.options.fancyGraphics;
    }

    private boolean shouldBob() {
        return true;
    }

    private boolean shouldSpreadItems() {
        return true;
    }

    @Override
    protected ResourceLocation getEntityTexture(final EntityItem entity) {
        return TextureMap.locationBlocksTexture;
    }
}