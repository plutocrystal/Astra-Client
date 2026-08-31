package dev.astra.module.impl.combat.killaura.render;

import dev.astra.module.impl.combat.killaura.KillAura;
import dev.astra.util.RenderUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class AttackRangeRenderer {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura parent;

    public AttackRangeRenderer(KillAura parent) {
        this.parent = parent;
    }

    public void render(float partialTicks) {
        if (!parent.isToggle()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        double radius = parent.range.getValue().doubleValue();
        if (radius <= 0.0D) return;
        float fr = parent.attackRangeFillColor.getRed() / 255.0f;
        float fg = parent.attackRangeFillColor.getGreen() / 255.0f;
        float fb = parent.attackRangeFillColor.getBlue() / 255.0f;
        float fa = parent.attackRangeFillColor.getAlpha() / 255.0f;
        float or = parent.attackRangeOutlineColor.getRed() / 255.0f;
        float og = parent.attackRangeOutlineColor.getGreen() / 255.0f;
        float ob = parent.attackRangeOutlineColor.getBlue() / 255.0f;
        float oa = parent.attackRangeOutlineColor.getAlpha() / 255.0f;
        double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks - RenderUtil.getCameraX(partialTicks);
        double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks - RenderUtil.getCameraY(partialTicks) - 0.05D;
        double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks - RenderUtil.getCameraZ(partialTicks);
        RenderUtil.setupCamera(partialTicks);
        for (int segments = 0; segments < 360; segments += 10) {
            double x1 = x - Math.sin(segments * Math.PI / 180F) * radius;
            double z1 = z + Math.cos(segments * Math.PI / 180F) * radius;
            double x2 = x - Math.sin((segments + 10) * Math.PI / 180F) * radius;
            double z2 = z + Math.cos((segments + 10) * Math.PI / 180F) * radius;
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(fr, fg, fb, fa);
            GL11.glVertex3d(x, y, z);
            GL11.glVertex3d(x1, y, z1);
            GL11.glVertex3d(x2, y, z2);
            GL11.glVertex3d(x, y, z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINES);
            GL11.glColor4f(or, og, ob, oa);
            GL11.glVertex3d(x1, y, z1);
            GL11.glVertex3d(x2, y, z2);
            GL11.glEnd();
        }
        RenderUtil.restoreCamera();
    }
}
