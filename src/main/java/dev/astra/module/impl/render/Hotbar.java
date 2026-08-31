package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.event.events.packet.PacketSendEvent;
import dev.astra.event.events.render.Render2DEvent;
import dev.astra.event.events.render.RenderHotbarEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.RenderUtil;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Hotbar extends Module {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final int CELL_WIDTH = 23;

    public static Hotbar INSTANCE;

    public final ColorValue backgroundColor = new ColorValue("Background", 0x77000000);

    public final ColorValue highlightColor = new ColorValue("Highlight", 0x66FFFFFF);

    public final BooleanValue time = new BooleanValue("Time", true);

    public final BooleanValue fps = new BooleanValue("FPS", true);

    public final BooleanValue bps = new BooleanValue("BPS", true);

    public final BooleanValue cps = new BooleanValue("CPS", true);

    public final BooleanValue leftCps = new BooleanValue("LeftCPS", true);

    public final BooleanValue rightCps = new BooleanValue("RightCPS", true);

    public final BooleanValue xyz = new BooleanValue("XYZ", true);

    public final BooleanValue ping = new BooleanValue("Ping", true);

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();

    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    private boolean lastRightDown = false;

    private double smoothedBps = 0.0D;

    public Hotbar() {
        super("Hotbar", Keyboard.KEY_NONE, Category.RENDER);
        addValues(backgroundColor, highlightColor, time, fps, bps, cps, leftCps, rightCps, xyz, ping);
        INSTANCE = this;
    }

    @EventHandler
    public void onRenderHotbar(RenderHotbarEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        event.setCancelled(true);
        ScaledResolution sr = event.getScaledResolution();
        float partialTicks = event.getPartialTicks();
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();
        EntityPlayer player = mc.thePlayer;

        RenderUtil.drawRect(0.0F, height - 23.0F, (float) width, (float) height, backgroundColor.getValue());

        double barX = width / 2.0D - 103.0D;
        double highlightX = barX + CELL_WIDTH * player.inventory.currentItem;
        RenderUtil.drawRect((float) highlightX, height - 23.0F,
                (float) (highlightX + CELL_WIDTH), (float) height, highlightColor.getValue());

        GL11.glPushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();
        for (int j = 0; j < 9; ++j) {
            double k = barX + 3.0D + CELL_WIDTH * j;
            double l = height - 23.0F + 3.0F;
            if (player.inventory.currentItem != j) {
                mc.ingameGUI.renderHotbarItem(j, (int) k, (int) l, partialTicks, player);
            } else {
                GL11.glPushMatrix();
                GL11.glScaled(1.05D, 1.05D, 1.0D);
                mc.ingameGUI.renderHotbarItem(j, (int) (k - k * 0.05D) + 1, (int) (l - l * 0.05D) + 1, partialTicks, player);
                GL11.glPopMatrix();
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null) return;
        double dx = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        double target = Math.hypot(dx, dz) * 20.0D;
        smoothedBps += (target - smoothedBps) * 0.25D;
    }

    private void trackClicks() {
        long now = System.currentTimeMillis();
        boolean rightDown = Mouse.isButtonDown(1);
        if (rightDown && !lastRightDown) {
            rightClicks.addLast(now);
        }
        lastRightDown = rightDown;
    }

    @EventHandler
    public void onPacket(PacketSendEvent event) {
        if (!(event.getPacket() instanceof C0APacketAnimation)) return;
        leftClicks.addLast(System.currentTimeMillis());
    }

    public void recordSwing() {
        leftClicks.addLast(System.currentTimeMillis());
    }

    private int countRecent(ArrayDeque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
            clicks.pollFirst();
        }
        return clicks.size();
    }

    private String getPing() {
        if (mc.getNetHandler() == null || mc.thePlayer == null) return "--";
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info == null) return "--";
        int responseTime = info.getResponseTime();
        return responseTime < 0 ? "--" : String.valueOf(responseTime);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        ScaledResolution sr = new ScaledResolution(mc);

        if (time.isEnabled()) {
            int right = sr.getScaledWidth() - 3;
            int bottom = sr.getScaledHeight();
            String dateStr = LocalDate.now().format(DATE_FORMAT);
            String timeStr = LocalTime.now().format(TIME_FORMAT);
            mc.fontRendererObj.drawStringWithShadow(timeStr, right - mc.fontRendererObj.getStringWidth(timeStr), bottom - 20, 0xFFFFFFFF);
            mc.fontRendererObj.drawStringWithShadow(dateStr, right - mc.fontRendererObj.getStringWidth(dateStr), bottom - 10, 0xFFFFFFFF);
        }

        trackClicks();

        List<String> topRow = new ArrayList<>();
        if (fps.isEnabled()) {
            topRow.add("FPS " + Minecraft.getDebugFPS());
        }
        if (bps.isEnabled()) {
            topRow.add("BPS " + String.format("%.2f", smoothedBps));
        }
        if (ping.isEnabled()) {
            topRow.add("Ping " + getPing());
        }

        List<String> bottomRow = new ArrayList<>();
        if (cps.isEnabled()) {
            boolean showLeft = leftCps.isEnabled();
            boolean showRight = rightCps.isEnabled();
            if (showLeft || showRight) {
                StringBuilder text = new StringBuilder("CPS ");
                if (showLeft) text.append(countRecent(leftClicks));
                if (showLeft && showRight) text.append('/');
                if (showRight) text.append(countRecent(rightClicks));
                bottomRow.add(text.toString());
            }
        }
        if (xyz.isEnabled()) {
            EntityPlayer player = mc.thePlayer;
            bottomRow.add("x " + MathHelper.floor_double(player.posX)
                    + " y " + MathHelper.floor_double(player.posY)
                    + " z " + MathHelper.floor_double(player.posZ));
        }

        int x = 3;
        int bottom = sr.getScaledHeight();
        if (!topRow.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow(String.join("  ", topRow), x, bottom - 20, 0xFFFFFFFF);
        }
        if (!bottomRow.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow(String.join("  ", bottomRow), x, bottom - 10, 0xFFFFFFFF);
        }
    }
}
