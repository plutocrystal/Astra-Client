package dev.astra.module.impl.render;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.attack.AttackEntityEvent;
import dev.astra.event.events.render.Render3DEvent;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.util.RenderUtil;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class TargetESP extends Module {
    private final ColorValue color = new ColorValue("Color", 0x80FFFFFF);

    private final ColorValue hurtColor = new ColorValue("HurtColor", 0xC8FF0000);

    private final BooleanValue onlyKillAura = new BooleanValue("OnlyKillAura", true);

    private final BooleanValue aimPoint = new BooleanValue("AimPoint", false);

    private final ColorValue aimFillColor = new ColorValue("FillColor", 0xFFFFFFFF);

    private final ColorValue aimOutlineColor = new ColorValue("OutlineColor", 0xFFFFFFFF);

    private final NumberValue aimSize = new NumberValue("AimSize", 0.15, 0.05, 1.0, 0.01);

    private final BooleanValue serverPos = new BooleanValue("ServerPos", true);

    private final ColorValue serverPosColor = new ColorValue("ServerColor", 0x60FFFF00);

    private EntityLivingBase target;

    private long lastTargetTime = -1;

    public TargetESP() {
        super("TargetESP", Keyboard.KEY_NONE, Category.RENDER);
        addValues(color, hurtColor, onlyKillAura, aimPoint, aimFillColor, aimOutlineColor, aimSize, serverPos, serverPosColor);
        refreshVisibility();
    }

    @Override
    public void refreshVisibility() {
        aimFillColor.setVisible(aimPoint.isEnabled());
        aimOutlineColor.setVisible(aimPoint.isEnabled());
        aimSize.setVisible(aimPoint.isEnabled());
        serverPosColor.setVisible(serverPos.isEnabled());
    }

    @Override
    public void onDisable() {
        target = null;
        lastTargetTime = -1;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            target = null;
            return;
        }
        KillAura killAura = Main.moduleManager != null ? Main.moduleManager.getModuleByClass(KillAura.class) : null;
        if (killAura != null && killAura.isToggle() && killAura.getAimTarget() != null) {
            target = killAura.getAimTarget();
            lastTargetTime = System.currentTimeMillis();
        }
        if (target != null && lastTargetTime != -1 && (target.isDead || System.currentTimeMillis() - lastTargetTime > 5000L || target.getDistanceSqToEntity(mc.thePlayer) > 20.0D)) {
            target = null;
            lastTargetTime = -1;
        }
        if (onlyKillAura.isEnabled()) return;
        if (target != null && (!inFov(target.posX, target.posZ, 180.0F) || target.getDistanceSqToEntity(mc.thePlayer) > 36.0D)) {
            target = null;
        }
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (onlyKillAura.isEnabled()) return;
        if (event.getTarget() instanceof EntityLivingBase) {
            target = (EntityLivingBase) event.getTarget();
            lastTargetTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        float partialTicks = event.getPartialTicks();
        double camX = RenderUtil.getCameraX(partialTicks);
        double camY = RenderUtil.getCameraY(partialTicks);
        double camZ = RenderUtil.getCameraZ(partialTicks);

        if (serverPos.isEnabled()) {
            RenderUtil.setupCamera(partialTicks);
            for (Object object : mc.theWorld.loadedEntityList) {
                if (!(object instanceof EntityLivingBase)) {
                    continue;
                }
                EntityLivingBase entity = (EntityLivingBase) object;
                if (entity == mc.thePlayer || entity.isDead) {
                    continue;
                }
                float sr = serverPosColor.getRed() / 255.0f;
                float sg = serverPosColor.getGreen() / 255.0f;
                float sb = serverPosColor.getBlue() / 255.0f;
                float sa = serverPosColor.getAlpha() / 255.0f;
                drawBox(getServerBox(entity), camX, camY, camZ, sr, sg, sb, sa);
            }
            RenderUtil.restoreCamera();
        }

        if (target == null) return;
        int drawTime = (int) (System.currentTimeMillis() % 2000L);
        boolean drawMode = drawTime > 1000;
        float drawPercent = drawTime / 1000f;
        if (!drawMode) {
            drawPercent = 1 - drawPercent;
        } else {
            drawPercent -= 1;
        }
        drawPercent *= 2;
        if (drawPercent < 1) {
            drawPercent = 0.5f * drawPercent * drawPercent * drawPercent;
        } else {
            float f = drawPercent - 2;
            drawPercent = 0.5f * (f * f * f + 2);
        }
        double radius = target.width;
        double height = target.height + 0.1;
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks - camX;
        double baseY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks - camY;
        double y = baseY + height * drawPercent;
        double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks - camZ;
        double eased = (height / 3) * ((drawPercent > 0.5) ? 1 - drawPercent : drawPercent) * (drawMode ? -1 : 1);
        ColorValue palette = target.hurtTime > 0 ? hurtColor : color;
        float r = palette.getRed() / 255.0f;
        float g = palette.getGreen() / 255.0f;
        float b = palette.getBlue() / 255.0f;
        float a = palette.getAlpha() / 255.0f;
        RenderUtil.setupCamera(partialTicks);
        for (int segments = 0; segments < 360; segments += 5) {
            double x1 = x - Math.sin(segments * Math.PI / 180F) * radius;
            double z1 = z + Math.cos(segments * Math.PI / 180F) * radius;
            double x2 = x - Math.sin((segments - 5) * Math.PI / 180F) * radius;
            double z2 = z + Math.cos((segments - 5) * Math.PI / 180F) * radius;
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(r, g, b, 0.0f);
            GL11.glVertex3d(x1, y + eased, z1);
            GL11.glVertex3d(x2, y + eased, z2);
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex3d(x2, y, z2);
            GL11.glVertex3d(x1, y, z1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex3d(x2, y, z2);
            GL11.glVertex3d(x1, y, z1);
            GL11.glEnd();
        }

        if (aimPoint.isEnabled()) {
            float ar = aimFillColor.getRed() / 255.0f;
            float ag = aimFillColor.getGreen() / 255.0f;
            float ab = aimFillColor.getBlue() / 255.0f;
            float aa = aimFillColor.getAlpha() / 255.0f;
            float oR = aimOutlineColor.getRed() / 255.0f;
            float oG = aimOutlineColor.getGreen() / 255.0f;
            float oB = aimOutlineColor.getBlue() / 255.0f;
            float oA = aimOutlineColor.getAlpha() / 255.0f;

            double halfBox = target.width / 2.0D;
            double eyeY = RenderUtil.getEyeHeight();
            double ax = Math.max(x - halfBox, Math.min(x + halfBox, 0.0D));
            double ay = Math.max(baseY, Math.min(baseY + target.height, eyeY));
            double az = Math.max(z - halfBox, Math.min(z + halfBox, 0.0D));

            double half = aimSize.getValue().doubleValue() / 2.0D;
            RenderUtil.drawCube(ax, ay, az, half, ar, ag, ab, aa, oR, oG, oB, oA);
        }

        RenderUtil.restoreCamera();
    }

    private AxisAlignedBB getServerBox(EntityLivingBase entity) {
        if (entity.serverPosX == 0 && entity.serverPosY == 0 && entity.serverPosZ == 0) {
            return entity.getEntityBoundingBox();
        }
        double sx = entity.serverPosX / 32.0D;
        double sy = entity.serverPosY / 32.0D;
        double sz = entity.serverPosZ / 32.0D;
        return new AxisAlignedBB(
                sx - entity.width / 2.0D, sy, sz - entity.width / 2.0D,
                sx + entity.width / 2.0D, sy + entity.height, sz + entity.width / 2.0D);
    }

    private void drawBox(AxisAlignedBB box, double camX, double camY, double camZ, float r, float g, float b, float a) {
        double x1 = box.minX - camX;
        double y1 = box.minY - camY;
        double z1 = box.minZ - camZ;
        double x2 = box.maxX - camX;
        double y2 = box.maxY - camY;
        double z2 = box.maxZ - camZ;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r, g, b, a * 0.2f);
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
        GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x2, y1, z2);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x2, y1, z2);
        GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x1, y2, z2);
        GL11.glEnd();

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
    }

    private boolean inFov(double posX, double posZ, float fov) {
        fov *= 0.5F;
        double diffX = posX - mc.thePlayer.posX;
        double diffZ = posZ - mc.thePlayer.posZ;
        double angle = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
        double fovToPoint = MathHelper.wrapAngleTo180_double(mc.thePlayer.rotationYaw - angle);
        return fovToPoint >= 0.0 ? fovToPoint < fov : fovToPoint > -fov;
    }
}
