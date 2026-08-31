package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.render.Render3DEvent;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.MultiBooleanValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Xray extends Module {
    public static Xray INSTANCE;

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

    public ModeValue mode = new ModeValue("Mode", "Invisible", "Invisible", "Outline");

    public MultiBooleanValue ores = new MultiBooleanValue("Ores")
            .add("Coal", true)
            .add("Iron", true)
            .add("Gold", true)
            .add("Diamond", true)
            .add("Emerald", true)
            .add("Redstone", true)
            .add("Lapis", true)
            .add("Quartz", true);

    public NumberValue radius = new NumberValue("Radius", 32, 8, 64, 4);

    public ColorValue coalColor = new ColorValue("CoalColor", 0x303030);

    public ColorValue ironColor = new ColorValue("IronColor", 0xD8AF93);

    public ColorValue goldColor = new ColorValue("GoldColor", 0xFCEE4B);

    public ColorValue diamondColor = new ColorValue("DiamondColor", 0x5DECF5);

    public ColorValue emeraldColor = new ColorValue("EmeraldColor", 0x17DD62);

    public ColorValue redstoneColor = new ColorValue("RedstoneColor", 0xFF0000);

    public ColorValue lapisColor = new ColorValue("LapisColor", 0x3052CE);

    public ColorValue quartzColor = new ColorValue("QuartzColor", 0xECECEC);

    private final List<Target> targets = new ArrayList<>();

    private World lastWorld;

    private String lastMode;

    private int lastRadius;

    private int tickCounter;

    public Xray() {
        super("Xray", Keyboard.KEY_NONE, Category.RENDER);
        addValues(mode, ores, radius,
                coalColor, ironColor, goldColor, diamondColor,
                emeraldColor, redstoneColor, lapisColor, quartzColor);
        INSTANCE = this;
        lastMode = mode.getValue();
        lastRadius = radius.getValue().intValue();
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    public boolean isTarget(Block block) {
        if (block == Blocks.coal_ore) return ores.getValue("Coal");
        if (block == Blocks.iron_ore) return ores.getValue("Iron");
        if (block == Blocks.gold_ore) return ores.getValue("Gold");
        if (block == Blocks.diamond_ore) return ores.getValue("Diamond");
        if (block == Blocks.emerald_ore) return ores.getValue("Emerald");
        if (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore) return ores.getValue("Redstone");
        if (block == Blocks.lapis_ore) return ores.getValue("Lapis");
        if (block == Blocks.quartz_ore) return ores.getValue("Quartz");
        return false;
    }

    public static boolean shouldHide(Block block) {
        if (INSTANCE == null || !INSTANCE.isToggle()) return false;
        if (!INSTANCE.mode.is("Invisible")) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        return !INSTANCE.isTarget(block);
    }

    private void reloadRenderers() {
        if (mc.renderGlobal != null && mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void onEnable() {
        lastMode = mode.getValue();
        lastRadius = radius.getValue().intValue();
        targets.clear();
        tickCounter = 0;
        reloadRenderers();
    }

    @Override
    public void onDisable() {
        targets.clear();
        reloadRenderers();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            lastWorld = null;
            return;
        }
        if (mc.theWorld != lastWorld) {
            lastWorld = mc.theWorld;
            targets.clear();
            tickCounter = 0;
            reloadRenderers();
            return;
        }
        if (!lastMode.equals(mode.getValue())) {
            lastMode = mode.getValue();
            targets.clear();
            reloadRenderers();
        }
        if (radius.getValue().intValue() != lastRadius) {
            lastRadius = radius.getValue().intValue();
            targets.clear();
        }
        if (mode.is("Outline") && ++tickCounter >= 10) {
            tickCounter = 0;
            scanTargets();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!mode.is("Outline") || mc.thePlayer == null || mc.theWorld == null || targets.isEmpty()) return;
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
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GL11.glLineWidth(1.5F);
        for (Target target : targets) {
            int color = getColor(target.block);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            GlStateManager.color(r, g, b, 1.0F);
            double x = target.pos.getX() - camX;
            double y = target.pos.getY() - camY;
            double z = target.pos.getZ() - camZ;
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(x, y, z);
            GL11.glVertex3d(x + 1.0D, y, z);
            GL11.glVertex3d(x + 1.0D, y, z);
            GL11.glVertex3d(x + 1.0D, y, z + 1.0D);
            GL11.glVertex3d(x + 1.0D, y, z + 1.0D);
            GL11.glVertex3d(x, y, z + 1.0D);
            GL11.glVertex3d(x, y, z + 1.0D);
            GL11.glVertex3d(x, y, z);
            GL11.glVertex3d(x, y + 1.0D, z);
            GL11.glVertex3d(x + 1.0D, y + 1.0D, z);
            GL11.glVertex3d(x + 1.0D, y + 1.0D, z);
            GL11.glVertex3d(x + 1.0D, y + 1.0D, z + 1.0D);
            GL11.glVertex3d(x + 1.0D, y + 1.0D, z + 1.0D);
            GL11.glVertex3d(x, y + 1.0D, z + 1.0D);
            GL11.glVertex3d(x, y + 1.0D, z + 1.0D);
            GL11.glVertex3d(x, y + 1.0D, z);
            GL11.glVertex3d(x, y, z);
            GL11.glVertex3d(x, y + 1.0D, z);
            GL11.glVertex3d(x + 1.0D, y, z);
            GL11.glVertex3d(x + 1.0D, y + 1.0D, z);
            GL11.glVertex3d(x + 1.0D, y, z + 1.0D);
            GL11.glVertex3d(x + 1.0D, y + 1.0D, z + 1.0D);
            GL11.glVertex3d(x, y, z + 1.0D);
            GL11.glVertex3d(x, y + 1.0D, z + 1.0D);
            GL11.glEnd();
        }
        GL11.glLineWidth(1.0F);
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void scanTargets() {
        targets.clear();
        int r = radius.getValue().intValue();
        BlockPos playerPos = new BlockPos(mc.thePlayer);
        int minY = playerPos.getY() - r < 0 ? -playerPos.getY() : -r;
        int maxY = playerPos.getY() + r > 255 ? 255 - playerPos.getY() : r;
        for (int x = -r; x <= r; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (isTarget(block)) {
                        targets.add(new Target(pos, block));
                    }
                }
            }
        }
    }

    private int getColor(Block block) {
        if (block == Blocks.coal_ore) return coalColor.getValue();
        if (block == Blocks.iron_ore) return ironColor.getValue();
        if (block == Blocks.gold_ore) return goldColor.getValue();
        if (block == Blocks.diamond_ore) return diamondColor.getValue();
        if (block == Blocks.emerald_ore) return emeraldColor.getValue();
        if (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore) return redstoneColor.getValue();
        if (block == Blocks.lapis_ore) return lapisColor.getValue();
        if (block == Blocks.quartz_ore) return quartzColor.getValue();
        return 0xFFFFFF;
    }

    private static final class Target {
        private final BlockPos pos;
        private final Block block;
        private Target(BlockPos pos, Block block) {
            this.pos = pos;
            this.block = block;
        }
    }
}
