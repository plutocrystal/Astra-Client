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

    private final KillAura parent;

    public DotRenderer(KillAura parent) {
        this.parent = parent;
    }

    public void render(float partialTicks) {
        if (!parent.dot.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        EntityLivingBase target = parent.getAimTarget();
        if (target == null || target.isDead) return;
        if (!parent.isWithinAimRange(target)) return;
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
        float r = parent.dotFillColor.getRed() / 255.0f;
        float g = parent.dotFillColor.getGreen() / 255.0f;
        float b = parent.dotFillColor.getBlue() / 255.0f;
        float a = parent.dotFillColor.getAlpha() / 255.0f;
        float oR = parent.dotOutlineColor.getRed() / 255.0f;
        float oG = parent.dotOutlineColor.getGreen() / 255.0f;
        float oB = parent.dotOutlineColor.getBlue() / 255.0f;
        float oA = parent.dotOutlineColor.getAlpha() / 255.0f;
        double half = parent.dotSize.getValue().doubleValue() / 2.0D;
        double dx = point.xCoord - RenderUtil.getCameraX(partialTicks);
        double dy = point.yCoord - RenderUtil.getCameraY(partialTicks);
        double dz = point.zCoord - RenderUtil.getCameraZ(partialTicks);
        RenderUtil.setupCamera(partialTicks);
        double cornerRadius = parent.dotCornerRadius.getValue().doubleValue();
        if (cornerRadius > 0.0D) {
            RenderUtil.drawRoundedCube(dx, dy, dz, half, cornerRadius, r, g, b, a, oR, oG, oB, oA);
        } else {
            RenderUtil.drawCube(dx, dy, dz, half, r, g, b, a, oR, oG, oB, oA);
        }
        RenderUtil.restoreCamera();
    }
}
