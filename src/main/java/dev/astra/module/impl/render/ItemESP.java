package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.render.Render3DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.lang.reflect.Method;

public class ItemESP extends Module {

    public static ItemESP INSTANCE;

    private static final Method GET_FOV_MODIFIER;

    static {
        Method method = null;
        try {
            method = EntityRenderer.class.getDeclaredMethod("getFOVModifier", float.class, boolean.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        GET_FOV_MODIFIER = method;
    }

    public final ModeValue mode = new ModeValue("Mode", "Box", "Box", "Direct");

    public final ColorValue outlineColor = new ColorValue("Outline", 0xFFFFFFFF);

    public final ColorValue fillColor = new ColorValue("Fill", 0x28FFFFFF);

    public final NumberValue boxSize = new NumberValue("BoxSize", 0.5, 0.25, 3.0, 0.05);

    public ItemESP() {
        super("ItemESP", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        addValues(mode, outlineColor, fillColor, boxSize);
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!mode.is("Box")) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float partialTicks = event.getPartialTicks();

        double camX = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks;
        double camY = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks + mc.thePlayer.getEyeHeight();
        double camZ = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks;
        float pitch = mc.thePlayer.prevRotationPitch + (mc.thePlayer.rotationPitch - mc.thePlayer.prevRotationPitch) * partialTicks;
        float yaw = mc.thePlayer.prevRotationYaw + (mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw) * partialTicks + 180.0F;
        float fov = mc.gameSettings.fovSetting;
        if (GET_FOV_MODIFIER != null) {
            try {
                fov = (Float) GET_FOV_MODIFIER.invoke(mc.entityRenderer, partialTicks, true);
            } catch (Exception ignored) {
            }
        }
        float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
        float clipDistance = Math.max(mc.gameSettings.renderDistanceChunks * 16.0F * 2.0F, 173.0F);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(fov, aspect, 0.05F, clipDistance);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5F);

        float or = outlineColor.getRed() / 255.0f;
        float og = outlineColor.getGreen() / 255.0f;
        float ob = outlineColor.getBlue() / 255.0f;
        float oa = outlineColor.getAlpha() / 255.0f;
        float fr = fillColor.getRed() / 255.0f;
        float fg = fillColor.getGreen() / 255.0f;
        float fb = fillColor.getBlue() / 255.0f;
        float fa = fillColor.getAlpha() / 255.0f;

        for (Object object : mc.theWorld.loadedEntityList) {
            if (!(object instanceof EntityItem)) {
                continue;
            }

            EntityItem entity = (EntityItem) object;

            if (entity.isDead) {
                continue;
            }

            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - camX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - camY;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - camZ;

            double half = boxSize.getValue().doubleValue() / 2.0D;
            double x1 = x - half;
            double x2 = x + half;
            double y1 = y - half;
            double y2 = y + half;
            double z1 = z - half;
            double z2 = z + half;

            if (fa > 0.0f) {
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glColor4f(fr, fg, fb, fa);
                GL11.glVertex3d(x1, y1, z1);
                GL11.glVertex3d(x1, y1, z2);
                GL11.glVertex3d(x2, y1, z2);
                GL11.glVertex3d(x2, y1, z1);
                GL11.glVertex3d(x1, y2, z1);
                GL11.glVertex3d(x2, y2, z1);
                GL11.glVertex3d(x2, y2, z2);
                GL11.glVertex3d(x1, y2, z2);
                GL11.glVertex3d(x1, y1, z2);
                GL11.glVertex3d(x1, y2, z2);
                GL11.glVertex3d(x2, y2, z2);
                GL11.glVertex3d(x2, y1, z2);
                GL11.glVertex3d(x2, y1, z1);
                GL11.glVertex3d(x2, y2, z1);
                GL11.glVertex3d(x1, y2, z1);
                GL11.glVertex3d(x1, y1, z1);
                GL11.glVertex3d(x1, y1, z1);
                GL11.glVertex3d(x1, y2, z1);
                GL11.glVertex3d(x1, y2, z2);
                GL11.glVertex3d(x1, y1, z2);
                GL11.glVertex3d(x2, y1, z2);
                GL11.glVertex3d(x2, y1, z1);
                GL11.glVertex3d(x2, y2, z1);
                GL11.glVertex3d(x2, y2, z2);
                GL11.glEnd();
            }

            if (oa > 0.0f) {
                GL11.glBegin(GL11.GL_LINES);
                GL11.glColor4f(or, og, ob, oa);
                GL11.glVertex3d(x1, y1, z1);
                GL11.glVertex3d(x2, y1, z1);
                GL11.glVertex3d(x2, y1, z1);
                GL11.glVertex3d(x2, y1, z2);
                GL11.glVertex3d(x2, y1, z2);
                GL11.glVertex3d(x1, y1, z2);
                GL11.glVertex3d(x1, y1, z2);
                GL11.glVertex3d(x1, y1, z1);
                GL11.glVertex3d(x1, y2, z1);
                GL11.glVertex3d(x2, y2, z1);
                GL11.glVertex3d(x2, y2, z1);
                GL11.glVertex3d(x2, y2, z2);
                GL11.glVertex3d(x2, y2, z2);
                GL11.glVertex3d(x1, y2, z2);
                GL11.glVertex3d(x1, y2, z2);
                GL11.glVertex3d(x1, y2, z1);
                GL11.glVertex3d(x1, y1, z1);
                GL11.glVertex3d(x1, y2, z1);
                GL11.glVertex3d(x2, y1, z1);
                GL11.glVertex3d(x2, y2, z1);
                GL11.glVertex3d(x2, y1, z2);
                GL11.glVertex3d(x2, y2, z2);
                GL11.glVertex3d(x1, y1, z2);
                GL11.glVertex3d(x1, y2, z2);
                GL11.glEnd();
            }
        }

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);

        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
