package dev.astra.module.impl.combat.killaura.render;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.util.RenderUtil;
import dev.astra.util.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class DotRenderer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long SMOOTH_HALF_LIFE_MS = 35L;
    private static final double TELEPORT_THRESHOLD_SQ = 4.0D;
    private static final long MAX_FRAME_DELTA_MS = 200L;

    private final KillAura parent;

    private double smoothX;
    private double smoothY;
    private double smoothZ;
    private boolean smoothInit;
    private long lastFrameMs;
    private EntityLivingBase lastTarget;

    public DotRenderer(KillAura parent) {
        this.parent = parent;
    }

    public void render(float partialTicks) {
        if (!parent.dot.isEnabled()) {
            resetSmoothing();
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            resetSmoothing();
            return;
        }
        EntityLivingBase target = parent.getAimTarget();
        if (target == null || target.isDead || !parent.isWithinAimRange(target)) {
            resetSmoothing();
            return;
        }

        Vec3 eyes = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 look = RotationUtils.getLookVector(parent.getRotationYaw(), parent.getRotationPitch());
        double swingRange = parent.swingRange.getValue().doubleValue();
        Vec3 end = eyes.addVector(look.xCoord * swingRange, look.yCoord * swingRange, look.zCoord * swingRange);
        double border = target.getCollisionBorderSize();
        AxisAlignedBB hitBox = target.getEntityBoundingBox().expand(
                border + parent.hBoxSize.getValue(),
                border + parent.vBoxSize.getValue(),
                border + parent.hBoxSize.getValue());
        MovingObjectPosition hit = hitBox.calculateIntercept(eyes, end);
        Vec3 point = hit != null && hit.hitVec != null ? hit.hitVec : end;

        double tx = point.xCoord - RenderUtil.getCameraX(partialTicks);
        double ty = point.yCoord - RenderUtil.getCameraY(partialTicks);
        double tz = point.zCoord - RenderUtil.getCameraZ(partialTicks);

        long now = System.currentTimeMillis();
        boolean newTarget = lastTarget != target;
        if (!smoothInit || newTarget) {
            smoothX = tx;
            smoothY = ty;
            smoothZ = tz;
            smoothInit = true;
            lastTarget = target;
            lastFrameMs = now;
        } else {
            long deltaMs = now - lastFrameMs;
            if (deltaMs < 0L) deltaMs = 0L;
            if (deltaMs > MAX_FRAME_DELTA_MS) deltaMs = MAX_FRAME_DELTA_MS;
            double dx = tx - smoothX;
            double dy = ty - smoothY;
            double dz = tz - smoothZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > TELEPORT_THRESHOLD_SQ) {
                smoothX = tx;
                smoothY = ty;
                smoothZ = tz;
            } else if (deltaMs > 0L) {
                double t = 1.0D - Math.pow(0.5D, (double) deltaMs / SMOOTH_HALF_LIFE_MS);
                if (t < 0.0D) t = 0.0D;
                if (t > 1.0D) t = 1.0D;
                smoothX += dx * t;
                smoothY += dy * t;
                smoothZ += dz * t;
            }
            lastFrameMs = now;
        }

        float r = parent.dotFillColor.getRed() / 255.0f;
        float g = parent.dotFillColor.getGreen() / 255.0f;
        float b = parent.dotFillColor.getBlue() / 255.0f;
        float a = parent.dotFillColor.getAlpha() / 255.0f;
        float oR = parent.dotOutlineColor.getRed() / 255.0f;
        float oG = parent.dotOutlineColor.getGreen() / 255.0f;
        float oB = parent.dotOutlineColor.getBlue() / 255.0f;
        float oA = parent.dotOutlineColor.getAlpha() / 255.0f;

        double half = parent.dotSize.getValue().doubleValue() / 2.0D;
        double cornerRadius = parent.dotCornerRadius.getValue().doubleValue();

        RenderUtil.setupCamera(partialTicks);
        if (cornerRadius > 0.0D) {
            RenderUtil.drawRoundedCube(smoothX, smoothY, smoothZ, half, cornerRadius, r, g, b, a, oR, oG, oB, oA);
        } else {
            RenderUtil.drawCube(smoothX, smoothY, smoothZ, half, r, g, b, a, oR, oG, oB, oA);
        }
        RenderUtil.restoreCamera();
    }

    private void resetSmoothing() {
        smoothInit = false;
        lastTarget = null;
        lastFrameMs = 0L;
    }
}