package dev.astra.util;

import dev.astra.Main;
import dev.astra.module.impl.render.CameraClip;
import dev.astra.module.impl.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RenderUtil {
    private static final int ARC_SEGMENTS = 10;

    private static final int SPHERE_LAT = 24;

    private static final int SPHERE_LON = 48;

    private static final int OUTLINE_RING_STEP = 30;

    private static final int OUTLINE_MERIDIAN_COUNT = 4;

    private static final int BLEND_SRC = 770;

    private static final int BLEND_DST = 771;

    private static final float THIRD_PERSON_DISTANCE = 4.0F;

    private static final Minecraft mc = Minecraft.getMinecraft();

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

    public static void setupCamera(float partialTicks) {
        float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
        float clipDistance = Math.max(mc.gameSettings.renderDistanceChunks * 16.0F * 2.0F, 173.0F);
        float fov = mc.gameSettings.fovSetting;
        if (GET_FOV_MODIFIER != null) {
            try {
                fov = (Float) GET_FOV_MODIFIER.invoke(mc.entityRenderer, partialTicks, true);
            } catch (Exception ignored) {
            }
        }
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(fov, aspect, 0.05F, clipDistance);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        hurtCameraEffect(partialTicks);
        if (mc.gameSettings.viewBobbing) {
            setupViewBobbing(partialTicks);
        }
        orientCamera(partialTicks);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }

    public static void restoreCamera() {
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glColor4f(1f, 1f, 1f, 1f);
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

    public static double getCameraX(float partialTicks) {
        Entity entity = getViewEntity();
        return entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
    }

    public static double getCameraY(float partialTicks) {
        Entity entity = getViewEntity();
        return entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
    }

    public static double getCameraZ(float partialTicks) {
        Entity entity = getViewEntity();
        return entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
    }

    public static float getEyeHeight() {
        return getViewEntity().getEyeHeight();
    }

    private static Entity getViewEntity() {
        return mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer;
    }

    private static void hurtCameraEffect(float partialTicks) {
        if (NoRender.hideHurtCam()) {
            return;
        }
        if (mc.getRenderViewEntity() instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) mc.getRenderViewEntity();
            float f = (float) living.hurtTime - partialTicks;
            if (living.getHealth() <= 0.0F) {
                float f1 = (float) living.deathTime + partialTicks;
                GlStateManager.rotate(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
            }
            if (f < 0.0F) {
                return;
            }
            f = f / (float) living.maxHurtTime;
            f = MathHelper.sin(f * f * f * f * (float) Math.PI);
            float f2 = living.attackedAtYaw;
            GlStateManager.rotate(-f2, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-f * 14.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f2, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void setupViewBobbing(float partialTicks) {
        if (mc.getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) mc.getRenderViewEntity();
            float f = player.distanceWalkedModified - player.prevDistanceWalkedModified;
            float f1 = -(player.distanceWalkedModified + f * partialTicks);
            float f2 = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
            float f3 = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * partialTicks;
            GlStateManager.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
            GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
        }
    }

    private static void orientCamera(float partialTicks) {
        Entity entity = getViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
            f = (float) ((double) f + 1.0D);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);
        } else if (mc.gameSettings.thirdPersonView > 0) {
            double d3 = THIRD_PERSON_DISTANCE;
            float f1 = entity.rotationYaw;
            float f2 = entity.rotationPitch;
            if (mc.gameSettings.thirdPersonView == 2) {
                f2 += 180.0F;
            }
            double d4 = (-MathHelper.sin(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d3;
            double d5 = (MathHelper.cos(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d3;
            double d6 = (-MathHelper.sin(f2 / 180.0F * (float) Math.PI)) * d3;
            CameraClip cameraClip = Main.moduleManager != null ? Main.moduleManager.getModuleByClass(CameraClip.class) : null;
            if (cameraClip == null || !cameraClip.isToggle()) {
                for (int i = 0; i < 8; ++i) {
                    float f3 = (float) ((i & 1) * 2 - 1) * 0.1F;
                    float f4 = (float) ((i >> 1 & 1) * 2 - 1) * 0.1F;
                    float f5 = (float) ((i >> 2 & 1) * 2 - 1) * 0.1F;
                    MovingObjectPosition movingobjectposition = mc.theWorld.rayTraceBlocks(new Vec3(d0 + f3, d1 + f4, d2 + f5),
                            new Vec3(d0 - d4 + f3 + f5, d1 - d6 + f4, d2 - d5 + f5));
                    if (movingobjectposition != null) {
                        double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));
                        if (d7 < d3) {
                            d3 = d7;
                        }
                    }
                }
            }
            if (mc.gameSettings.thirdPersonView == 2) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }
            GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
            GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
        } else {
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
        }
        GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);
        if (entity instanceof EntityAnimal) {
            EntityAnimal animal = (EntityAnimal) entity;
            GlStateManager.rotate(animal.prevRotationYawHead + (animal.rotationYawHead - animal.prevRotationYawHead) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
        } else {
            GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
        }
        GlStateManager.translate(0.0F, -f, 0.0F);
    }

    public static void drawCube(double cx, double cy, double cz, double half, float r, float g, float b, float a) {
        drawCube(cx, cy, cz, half, r, g, b, a, r, g, b, a);
    }

    public static void drawCube(double cx, double cy, double cz, double half,
                                float fR, float fG, float fB, float fA,
                                float oR, float oG, float oB, float oA) {
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_CULL_FACE);
        drawCubeFill(cx, cy, cz, half, fR, fG, fB, fA);
        if (!wasCullEnabled) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        drawCubeOutline(cx, cy, cz, half, oR, oG, oB, oA);
    }

    private static void drawCubeFill(double cx, double cy, double cz, double half, float r, float g, float b, float a) {
        double x1 = cx - half;
        double x2 = cx + half;
        double y1 = cy - half;
        double y2 = cy + half;
        double z1 = cz - half;
        double z2 = cz + half;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r, g, b, a);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x2, y1, z2);
        GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x2, y1, z2);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x2, y1, z2);
        GL11.glEnd();
    }

    private static void drawCubeOutline(double cx, double cy, double cz, double half, float r, float g, float b, float a) {
        double x1 = cx - half;
        double x2 = cx + half;
        double y1 = cy - half;
        double y2 = cy + half;
        double z1 = cz - half;
        double z2 = cz + half;
        boolean wasSmoothEnabled = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(r, g, b, a);
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
        if (wasSmoothEnabled) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
        }
    }

    private static void drawBall(double cx, double cy, double cz, double radius, float r, float g, float b, float a) {
        if (radius <= 0.0D || a <= 0.0F) {
            return;
        }
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r, g, b, a);
        for (int i = 0; i < SPHERE_LAT; i++) {
            double lat1 = Math.PI * i / SPHERE_LAT - Math.PI / 2.0D;
            double lat2 = Math.PI * (i + 1) / SPHERE_LAT - Math.PI / 2.0D;
            double sin1 = Math.sin(lat1);
            double sin2 = Math.sin(lat2);
            double cos1 = Math.cos(lat1);
            double cos2 = Math.cos(lat2);
            for (int j = 0; j < SPHERE_LON; j++) {
                double lon1 = 2.0D * Math.PI * j / SPHERE_LON;
                double lon2 = 2.0D * Math.PI * (j + 1) / SPHERE_LON;
                double x11 = Math.cos(lon1) * cos1;
                double z11 = Math.sin(lon1) * cos1;
                double x21 = Math.cos(lon2) * cos1;
                double z21 = Math.sin(lon2) * cos1;
                double x12 = Math.cos(lon1) * cos2;
                double z12 = Math.sin(lon1) * cos2;
                double x22 = Math.cos(lon2) * cos2;
                double z22 = Math.sin(lon2) * cos2;
                GL11.glVertex3d(cx + x11 * radius, cy + sin1 * radius, cz + z11 * radius);
                GL11.glVertex3d(cx + x12 * radius, cy + sin2 * radius, cz + z12 * radius);
                GL11.glVertex3d(cx + x22 * radius, cy + sin2 * radius, cz + z22 * radius);
                GL11.glVertex3d(cx + x21 * radius, cy + sin1 * radius, cz + z21 * radius);
            }
        }
        GL11.glEnd();
    }

    public static void drawRoundedCube(double cx, double cy, double cz, double half, double radius,
                                       float fR, float fG, float fB, float fA,
                                       float oR, float oG, float oB, float oA) {
        if (radius <= 0.0D) {
            drawCube(cx, cy, cz, half, fR, fG, fB, fA, oR, oG, oB, oA);
            return;
        }
        double core = half - radius;
        if (core <= 0.0D) {
            drawBall(cx, cy, cz, half, fR, fG, fB, fA);
            return;
        }
        double probe = 2.0D * half + radius;
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(fR, fG, fB, fA);
        for (int i = 0; i < SPHERE_LAT; i++) {
            double lat1 = Math.PI * i / SPHERE_LAT - Math.PI / 2.0D;
            double lat2 = Math.PI * (i + 1) / SPHERE_LAT - Math.PI / 2.0D;
            double dy1 = Math.sin(lat1);
            double dy2 = Math.sin(lat2);
            double rad1 = Math.cos(lat1);
            double rad2 = Math.cos(lat2);
            for (int j = 0; j < SPHERE_LON; j++) {
                double lon1 = 2.0D * Math.PI * j / SPHERE_LON;
                double lon2 = 2.0D * Math.PI * (j + 1) / SPHERE_LON;
                roundedCubeVertex(Math.cos(lon1) * rad1, dy1, Math.sin(lon1) * rad1, cx, cy, cz, core, radius, probe);
                roundedCubeVertex(Math.cos(lon1) * rad2, dy2, Math.sin(lon1) * rad2, cx, cy, cz, core, radius, probe);
                roundedCubeVertex(Math.cos(lon2) * rad2, dy2, Math.sin(lon2) * rad2, cx, cy, cz, core, radius, probe);
                roundedCubeVertex(Math.cos(lon2) * rad1, dy1, Math.sin(lon2) * rad1, cx, cy, cz, core, radius, probe);
            }
        }
        GL11.glEnd();
        if (!wasCullEnabled) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        if (oA > 0.0F) {
            GL11.glColor4f(oR, oG, oB, oA);
            for (int deg = -60; deg <= 60; deg += OUTLINE_RING_STEP) {
                double latRad = Math.toRadians(deg);
                double ringDy = Math.sin(latRad);
                double ringRad = Math.cos(latRad);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int k = 0; k <= SPHERE_LON; k++) {
                    double lon = 2.0D * Math.PI * k / SPHERE_LON;
                    roundedCubeVertex(Math.cos(lon) * ringRad, ringDy, Math.sin(lon) * ringRad,
                            cx, cy, cz, core, radius, probe);
                }
                GL11.glEnd();
            }
            for (int m = 0; m < OUTLINE_MERIDIAN_COUNT; m++) {
                double psi = Math.PI * m / OUTLINE_MERIDIAN_COUNT;
                double ca = Math.cos(psi);
                double sa = Math.sin(psi);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int k = 0; k <= SPHERE_LON; k++) {
                    double t = 2.0D * Math.PI * k / SPHERE_LON;
                    roundedCubeVertex(ca * Math.cos(t), Math.sin(t), sa * Math.cos(t),
                            cx, cy, cz, core, radius, probe);
                }
                GL11.glEnd();
            }
        }
    }

    private static void roundedCubeVertex(double dx, double dy, double dz,
                                          double cx, double cy, double cz,
                                          double core, double radius, double probe) {
        double px = dx * probe;
        double py = dy * probe;
        double pz = dz * probe;
        double qx = clampAbs(px, core);
        double qy = clampAbs(py, core);
        double qz = clampAbs(pz, core);
        double rx = px - qx;
        double ry = py - qy;
        double rz = pz - qz;
        double len = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (len < 1.0E-7D) {
            rx = dx;
            ry = dy;
            rz = dz;
            len = 1.0D;
        }
        GL11.glVertex3d(cx + qx + rx / len * radius,
                        cy + qy + ry / len * radius,
                        cz + qz + rz / len * radius);
    }

    private static double clampAbs(double value, double limit) {
        return Math.max(-limit, Math.min(limit, value));
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left >= right || top >= bottom) {
            return;
        }
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(BLEND_SRC, BLEND_DST, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(worldrenderer, left, top, color);
        vertex(worldrenderer, left, bottom, color);
        vertex(worldrenderer, right, bottom, color);
        vertex(worldrenderer, right, top, color);
        tessellator.draw();
        restoreCull(wasCullEnabled);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawSquare(int x, int y, int size, int color) {
        Gui.drawRect(x, y, x + size, y + size, color);
    }

    public static void drawCircle(float centerX, float centerY, float radius, int color) {
        if (radius <= 0.0f) {
            return;
        }
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(BLEND_SRC, BLEND_DST, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(worldrenderer, centerX, centerY, color);
        for (int i = 0; i <= 360; i += 6) {
            double rad = Math.toRadians(i);
            vertex(worldrenderer, centerX + (float) Math.cos(rad) * radius,
                    centerY + (float) Math.sin(rad) * radius, color);
        }
        tessellator.draw();
        restoreCull(wasCullEnabled);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawTriangle(float x, float y, float size, boolean up, int color) {
        if (size <= 0.0f) {
            return;
        }
        float tipY = up ? y - size : y + size;
        float baseY = up ? y + size : y - size;
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(BLEND_SRC, BLEND_DST, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        vertex(worldrenderer, x, baseY, color);
        vertex(worldrenderer, x + size * 2.0f, baseY, color);
        vertex(worldrenderer, x + size, tipY, color);
        tessellator.draw();
        restoreCull(wasCullEnabled);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawOutline(int left, int top, int right, int bottom, int color) {
        int x1 = Math.min(left, right);
        int x2 = Math.max(left, right);
        int y1 = Math.min(top, bottom);
        int y2 = Math.max(top, bottom);
        Gui.drawRect(x1, y1, x2, y1 + 1, color);
        Gui.drawRect(x1, y2 - 1, x2, y2, color);
        Gui.drawRect(x1, y1, x1 + 1, y2, color);
        Gui.drawRect(x2 - 1, y1, x2, y2, color);
    }

    public static void drawHorizontalLine(int startX, int endX, int y, int color) {
        Gui.drawRect(Math.min(startX, endX), y, Math.max(startX, endX) + 1, y + 1, color);
    }

    public static void drawVerticalLine(int x, int startY, int endY, int color) {
        Gui.drawRect(x, Math.min(startY, endY), x + 1, Math.max(startY, endY) + 1, color);
    }

    public static void drawRoundedRect(int left, int top, int right, int bottom, int radius, int color) {
        int x1 = Math.min(left, right);
        int x2 = Math.max(left, right);
        int y1 = Math.min(top, bottom);
        int y2 = Math.max(top, bottom);
        if (radius > (y2 - y1) / 2 || radius > (x2 - x1) / 2) {
            radius = Math.min((y2 - y1) / 2, (x2 - x1) / 2);
        }
        if (radius <= 0) {
            Gui.drawRect(x1, y1, x2, y2, color);
            return;
        }
        Gui.drawRect(x1 + radius, y1, x2 - radius, y2, color);
        Gui.drawRect(x1, y1 + radius, x2, y2 - radius, color);
        for (int i = 0; i < radius; i++) {
            int width = (int) Math.round(radius * Math.sin(Math.toRadians(90.0 * i / radius)));
            Gui.drawRect(x1 + radius - width, y1 + i, x2 - radius + width, y1 + i + 1, color);
            Gui.drawRect(x1 + radius - width, y2 - i - 1, x2 - radius + width, y2 - i, color);
        }
    }

    public static void drawRoundedRect(float left, float top, float right, float bottom, float radius, int color) {
        drawRoundedRect(left, top, right, bottom, radius, color, true, true, true, true);
    }

    public static void drawRoundedRect(float left, float top, float right, float bottom, float radius, int color,
                                       boolean roundTopLeft, boolean roundTopRight, boolean roundBottomRight, boolean roundBottomLeft) {
        float x1 = Math.min(left, right);
        float x2 = Math.max(left, right);
        float y1 = Math.min(top, bottom);
        float y2 = Math.max(top, bottom);
        float maxRadius = Math.min(x2 - x1, y2 - y1) / 2.0f;
        float rTL = roundTopLeft ? Math.min(radius, maxRadius) : 0.0f;
        float rTR = roundTopRight ? Math.min(radius, maxRadius) : 0.0f;
        float rBR = roundBottomRight ? Math.min(radius, maxRadius) : 0.0f;
        float rBL = roundBottomLeft ? Math.min(radius, maxRadius) : 0.0f;
        if (rTL <= 0.0f && rTR <= 0.0f && rBR <= 0.0f && rBL <= 0.0f) {
            drawRect(x1, y1, x2, y2, color);
            return;
        }
        List<float[]> outline = new ArrayList<>();
        addArc(outline, x1 + rTL, y1 + rTL, rTL, 180.0f, 270.0f);
        addArc(outline, x2 - rTR, y1 + rTR, rTR, 270.0f, 360.0f);
        addArc(outline, x2 - rBR, y2 - rBR, rBR, 0.0f, 90.0f);
        addArc(outline, x1 + rBL, y2 - rBL, rBL, 90.0f, 180.0f);
        float centerX = (x1 + x2) / 2.0f;
        float centerY = (y1 + y2) / 2.0f;
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(BLEND_SRC, BLEND_DST, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < outline.size(); i++) {
            float[] a = outline.get(i);
            float[] b = outline.get((i + 1) % outline.size());
            vertex(worldrenderer, centerX, centerY, color);
            vertex(worldrenderer, a[0], a[1], color);
            vertex(worldrenderer, b[0], b[1], color);
        }
        tessellator.draw();
        restoreCull(wasCullEnabled);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawRoundedRectOutline(float left, float top, float right, float bottom, float radius, int color) {
        float x1 = Math.min(left, right);
        float x2 = Math.max(left, right);
        float y1 = Math.min(top, bottom);
        float y2 = Math.max(top, bottom);
        float maxRadius = Math.min(x2 - x1, y2 - y1) / 2.0f;
        float r = Math.min(radius, maxRadius);
        if (r <= 0.0f) {
            drawOutline((int) x1, (int) y1, (int) x2, (int) y2, color);
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(BLEND_SRC, BLEND_DST, 1, 0);
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(1.0f);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        addArcVertices(worldrenderer, x1 + r, y1 + r, r, 180.0f, 270.0f, color);
        addArcVertices(worldrenderer, x2 - r, y1 + r, r, 270.0f, 360.0f, color);
        addArcVertices(worldrenderer, x2 - r, y2 - r, r, 0.0f, 90.0f, color);
        addArcVertices(worldrenderer, x1 + r, y2 - r, r, 90.0f, 180.0f, color);
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void addArc(List<float[]> outline, float centerX, float centerY, float radius,
                               float startAngle, float endAngle) {
        for (int i = 0; i <= ARC_SEGMENTS; i++) {
            float angle = (float) Math.toRadians(startAngle + (endAngle - startAngle) * i / ARC_SEGMENTS);
            outline.add(new float[]{centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius});
        }
    }

    private static void addArcVertices(WorldRenderer worldrenderer, float centerX, float centerY, float radius,
                                       float startAngle, float endAngle, int color) {
        for (int i = 0; i <= ARC_SEGMENTS; i++) {
            float angle = (float) Math.toRadians(startAngle + (endAngle - startAngle) * i / ARC_SEGMENTS);
            vertex(worldrenderer, centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius, color);
        }
    }

    private static void vertex(WorldRenderer worldrenderer, float x, float y, int color) {
        worldrenderer.pos(x, y, 0.0F)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .endVertex();
    }

    private static void restoreCull(boolean wasEnabled) {
        if (wasEnabled) {
            GlStateManager.enableCull();
        }
    }
}
