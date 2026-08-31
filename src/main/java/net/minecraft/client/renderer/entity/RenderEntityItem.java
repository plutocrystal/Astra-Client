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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class RenderEntityItem extends Render<EntityItem> {
    private final RenderItem itemRenderer;
    private Random field_177079_e;

    public RenderEntityItem(final RenderManager renderManagerIn, final RenderItem p_i46167_2_) {
        super(renderManagerIn);
        this.field_177079_e = new Random();
        this.itemRenderer = p_i46167_2_;
        this.shadowSize = 0.15f;
        this.shadowOpaque = 0.75f;
    }

    private int func_177077_a(final EntityItem itemIn, final double p_177077_2_, final double p_177077_4_, final double p_177077_6_, final float p_177077_8_, final IBakedModel p_177077_9_) {
        final ItemStack itemstack = itemIn.getEntityItem();
        final Item item = itemstack.getItem();
        if (item == null) {
            return 0;
        }
        final boolean flag = p_177077_9_.isGui3d();
        final int i = isCountEnabled() ? 1 : this.func_177078_a(itemstack);
        float f2 = MathHelper.sin((itemIn.getAge() + p_177077_8_) / 10.0f + itemIn.hoverStart) * 0.1f + 0.1f;

        boolean isPhysics = ItemPhysics.INSTANCE != null && ItemPhysics.INSTANCE.isToggle();
        boolean isPhysicsMode = isPhysics && ItemPhysics.mode.is("Physics");
        boolean is17Mode = isPhysics && ItemPhysics.mode.is("1.7");
        boolean is2DItem = !flag;

        if (isPhysicsMode) {
            f2 = 0.0f;
        }

        final float f3 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
        if (is17Mode && is2DItem) {
            GlStateManager.translate((float)p_177077_2_, (float)p_177077_4_ + f2 + 0.05f * f3 + 0.2f, (float)p_177077_6_);
        } else if (isPhysicsMode) {
            GlStateManager.translate((float)p_177077_2_, (float)p_177077_4_ + f2 + 0.05f * f3, (float)p_177077_6_);
        } else {
            GlStateManager.translate((float)p_177077_2_, (float)p_177077_4_ + f2 + 0.25f * f3, (float)p_177077_6_);
        }

        if (flag || this.renderManager.options != null) {
            final float f4 = ((itemIn.getAge() + p_177077_8_) / 20.0f + itemIn.hoverStart) * 57.295776f;
            if (is17Mode && is2DItem) {
                GlStateManager.rotate(180.0f - this.renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(-this.renderManager.playerViewX, 1.0f, 0.0f, 0.0f);
            } else if (isPhysicsMode) {
                if (itemIn.onGround) {
                    GL11.glRotatef(itemIn.rotationYaw, 0.0f, 1.0f, 0.0f);
                    GL11.glRotatef(itemIn.rotationPitch + 90.0f, 1.0f, 0.0f, 0.0f);
                } else {
                    for (int a = 0; a < 10; ++a) {
                        GL11.glRotatef(f4, 0.7f, 0.7f, 0.0f);
                    }
                }
            } else {
                GlStateManager.rotate(f4, 0.0f, 1.0f, 0.0f);
            }
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

    @Override
    public void doRender(final EntityItem entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks) {
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
        boolean espDirect = dev.astra.module.impl.render.ItemESP.INSTANCE != null
                && dev.astra.module.impl.render.ItemESP.INSTANCE.isToggle()
                && dev.astra.module.impl.render.ItemESP.INSTANCE.mode.is("Direct");
        if (espDirect) {
            GlStateManager.disableDepth();
        }
        GlStateManager.pushMatrix();
        final IBakedModel ibakedmodel = this.itemRenderer.getItemModelMesher().getItemModel(itemstack);
        boolean isPhysics = ItemPhysics.INSTANCE != null && ItemPhysics.INSTANCE.isToggle();
        float size = isPhysics ? ItemPhysics.scale.getValue().floatValue() : 1.0f;
        for (int i = this.func_177077_a(entity, x, y, z, partialTicks, ibakedmodel), j = 0; j < i; ++j) {
            if (ibakedmodel.isGui3d()) {
                GlStateManager.pushMatrix();
                if (j > 0) {
                    final float f = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    final float f2 = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    final float f3 = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    GlStateManager.translate(f, f2, f3);
                }
                GlStateManager.scale(0.5f * size, 0.5f * size, 0.5f * size);
                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                this.itemRenderer.renderItem(itemstack, ibakedmodel);
                GlStateManager.popMatrix();
            } else {
                GlStateManager.pushMatrix();
                GlStateManager.scale(size, size, size);
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
        if (espDirect) {
            GlStateManager.enableDepth();
        }
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        this.bindEntityTexture(entity);
        if (flag) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).restoreLastBlurMipmap();
        }
        this.renderItemOverlay(entity, x, y, z, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void renderItemOverlay(EntityItem entity, double x, double y, double z, float partialTicks) {
        ItemStack itemstack = entity.getEntityItem();

        if (itemstack == null || itemstack.getItem() == null) {
            return;
        }

        boolean moduleOn = ItemPhysics.INSTANCE != null && ItemPhysics.INSTANCE.isToggle();
        boolean drawCount = moduleOn && ItemPhysics.showCount.isEnabled() && itemstack.stackSize > 1;
        boolean drawDurability = moduleOn && ItemPhysics.showDurability.isEnabled() && itemstack.isItemStackDamageable() && itemstack.getItemDamage() > 0;

        if (!drawCount && !drawDurability) {
            return;
        }

        FontRenderer fontRenderer = this.getFontRendererFromRenderManager();

        if (fontRenderer == null) {
            return;
        }

        float bob = MathHelper.sin((entity.getAge() + partialTicks) / 10.0f + entity.hoverStart) * 0.1f + 0.1f;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + bob + 0.45f, (float) z);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-0.02f, -0.02f, 0.02f);
        GlStateManager.translate(-8.0f, 0.0f, 0.0f);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (drawDurability) {
            int i = (int) Math.round(13.0d - (double) itemstack.getItemDamage() * 13.0d / (double) itemstack.getMaxDamage());
            int j = (int) Math.round(255.0d - (double) itemstack.getItemDamage() * 255.0d / (double) itemstack.getMaxDamage());
            GlStateManager.disableTexture2D();
            this.drawOverlayQuad(2, 13, 13, 2, 0, 0, 0, 255);
            this.drawOverlayQuad(2, 13, 12, 1, (255 - j) / 4, 64, 0, 255);
            this.drawOverlayQuad(2, 13, i, 1, 255 - j, j, 0, 255);
            GlStateManager.enableTexture2D();
        }

        if (drawCount) {
            String s = String.valueOf(itemstack.stackSize);
            fontRenderer.drawStringWithShadow(s, 17.0f - (float) fontRenderer.getStringWidth(s), 9.0f, 16777215);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawOverlayQuad(int x, int y, int width, int height, int red, int green, int blue, int alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x, y, 0.0d).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x, y + height, 0.0d).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0d).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + width, y, 0.0d).color(red, green, blue, alpha).endVertex();
        Tessellator.getInstance().draw();
    }

    private static boolean isCountEnabled() {
        return ItemPhysics.INSTANCE != null && ItemPhysics.INSTANCE.isToggle() && ItemPhysics.showCount.isEnabled();
    }

    @Override
    protected ResourceLocation getEntityTexture(final EntityItem entity) {
        return TextureMap.locationBlocksTexture;
    }
}