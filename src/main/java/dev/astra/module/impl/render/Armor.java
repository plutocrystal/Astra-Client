package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.render.Render2DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.RenderUtil;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.text.DecimalFormat;

public class Armor extends Module {

    public final ModeValue mode = new ModeValue("Mode", "Horizontal", "Horizontal", "Vertical");

    public final ModeValue durability = new ModeValue("Durability", "All", "All", "None", "Percentage", "Value");

    public final ModeValue colorMode = new ModeValue("ColorMode", "Damage", "Damage", "Custom");

    public final ColorValue customColor = new ColorValue("CustomColor", 0xFFFFFFFF);

    public final NumberValue scale = new NumberValue("Scale", 1.0, 0.5, 2.0, 0.05);

    private final NumberValue anchorX = new NumberValue("AnchorX", 0.5, 0, 1, 0.001);

    private final NumberValue anchorY = new NumberValue("AnchorY", 0.75, 0, 1, 0.001);

    private boolean positionInitialized = false;

    private boolean dragging = false;

    private boolean mouseOver = false;

    private boolean lastMiddleDown = false;

    private int lastMouseX;

    private int lastMouseY;

    private float boundsLeft;

    private float boundsTop;

    private float boundsRight;

    private float boundsBottom;

    private static final float OUTLINE_RADIUS = 3.0F;

    private static final int CELL = 18;

    private static final int ITEM_SIZE = 16;

    private static final int TEXT_GAP = 2;

    private static final int COLOR_GREEN = 0x00FF00;

    private static final int COLOR_ORANGE = 0xFFA500;

    private static final int COLOR_YELLOW = 0xFFFF00;

    private static final int COLOR_RED = 0xFF0000;

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("###0.00");

    public Armor() {
        super("Armor", Keyboard.KEY_NONE, Category.RENDER);
        anchorX.setVisible(false);
        anchorY.setVisible(false);
        addValues(mode, durability, colorMode, customColor, scale, anchorX, anchorY);
        refreshVisibility();
    }

    @Override
    public void refreshVisibility() {
        customColor.setVisible(colorMode.is("Custom"));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        dragging = false;
        mouseOver = false;
        lastMiddleDown = false;
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        ItemStack[] armor = mc.thePlayer.inventory.armorInventory;
        boolean showText = !durability.is("None");

        String[] texts = new String[4];
        int maxTextWidth = 0;
        for (int slot = 0; slot < 4; slot++) {
            texts[slot] = showText ? buildDurabilityText(armor[slot]) : null;
            if (texts[slot] != null) {
                int w = mc.fontRendererObj.getStringWidth(texts[slot]);
                if (w > maxTextWidth) maxTextWidth = w;
            }
        }

        boolean vertical = mode.is("Vertical");
        float contentW = vertical
                ? ITEM_SIZE + (maxTextWidth > 0 ? TEXT_GAP + maxTextWidth : 0)
                : 4 * CELL;
        float contentH = vertical
                ? 4 * CELL
                : ITEM_SIZE + (maxTextWidth > 0 ? TEXT_GAP + mc.fontRendererObj.FONT_HEIGHT : 0);

        float s = scale.getValue().floatValue();
        float halfW = contentW * s / 2.0F;
        float halfH = contentH * s / 2.0F;

        if (!positionInitialized) {
            anchorX.setValue((double) clamp01(0.5F));
            anchorY.setValue((double) clamp01((screenH - 45.0F - halfH) / screenH));
            positionInitialized = true;
        }

        float minX = halfW;
        float maxX = screenW - halfW;
        float cx = minX > maxX ? (minX + maxX) / 2.0F : Math.max(minX, Math.min(maxX, anchorX.getValue().floatValue() * screenW));

        float minY = halfH;
        float maxY = screenH - halfH;
        float cy = minY > maxY ? (minY + maxY) / 2.0F : Math.max(minY, Math.min(maxY, anchorY.getValue().floatValue() * screenH));

        float transX = cx - halfW;
        float transY = cy - halfH;
        boundsLeft = transX;
        boundsTop = transY;
        boundsRight = transX + contentW * s;
        boundsBottom = transY + contentH * s;

        mouseOver = false;
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        if (!chatOpen) {
            dragging = false;
            lastMiddleDown = false;
        } else {
            int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
            mouseOver = mouseX >= boundsLeft && mouseX <= boundsRight && mouseY >= boundsTop && mouseY <= boundsBottom;

            boolean leftDown = Mouse.isButtonDown(0);

            if (leftDown && !dragging && mouseOver) {
                dragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }

            if (dragging) {
                if (!leftDown) {
                    dragging = false;
                } else {
                    float dx = mouseX - lastMouseX;
                    float dy = mouseY - lastMouseY;

                    if (dx != 0.0F || dy != 0.0F) {
                        if (dx != 0.0F) {
                            anchorX.setValue((double) clamp01(cx / screenW + dx / screenW));
                        }
                        if (dy != 0.0F) {
                            anchorY.setValue((double) clamp01(cy / screenH + dy / screenH));
                        }
                        cx = Math.max(minX, Math.min(maxX, anchorX.getValue().floatValue() * screenW));
                        cy = Math.max(minY, Math.min(maxY, anchorY.getValue().floatValue() * screenH));
                        transX = cx - halfW;
                        transY = cy - halfH;
                        boundsLeft = transX;
                        boundsTop = transY;
                        boundsRight = transX + contentW * s;
                        boundsBottom = transY + contentH * s;
                    }

                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                }
            }

            boolean middleDown = Mouse.isButtonDown(2);

            if (middleDown && !lastMiddleDown && mouseOver) {
                anchorX.setValue((double) clamp01(0.5F));
                anchorY.setValue((double) clamp01((screenH - 45.0F - halfH) / screenH));
                cx = Math.max(minX, Math.min(maxX, anchorX.getValue().floatValue() * screenW));
                cy = Math.max(minY, Math.min(maxY, anchorY.getValue().floatValue() * screenH));
                transX = cx - halfW;
                transY = cy - halfH;
                boundsLeft = transX;
                boundsTop = transY;
                boundsRight = transX + contentW * s;
                boundsBottom = transY + contentH * s;
            }

            lastMiddleDown = middleDown;
        }

        if (dragging || mouseOver) {
            int alpha = dragging ? 235 : 130;
            RenderUtil.drawRoundedRectOutline(boundsLeft, boundsTop, boundsRight, boundsBottom,
                    OUTLINE_RADIUS, (alpha << 24) | 0xFFFFFF);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(transX, transY, 0.0F);
        GlStateManager.scale(s, s, 1.0F);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        for (int cell = 0; cell < 4; cell++) {
            int slot = 3 - cell;
            ItemStack stack = armor[slot];
            if (stack == null) continue;

            int ix;
            int iy;
            if (vertical) {
                ix = 0;
                iy = cell * CELL + 1;
            } else {
                ix = cell * CELL + 1;
                iy = 0;
            }

            mc.getRenderItem().renderItemIntoGUI(stack, ix, iy);
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, ix, iy);

            if (texts[cell] != null) {
                int color = durabilityColor(stack);
                if (vertical) {
                    mc.fontRendererObj.drawStringWithShadow(texts[cell], ITEM_SIZE + TEXT_GAP, iy + 4, color);
                } else {
                    int textX = cell * CELL + (ITEM_SIZE - mc.fontRendererObj.getStringWidth(texts[cell])) / 2;
                    mc.fontRendererObj.drawStringWithShadow(texts[cell], textX, ITEM_SIZE + TEXT_GAP, color);
                }
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private String buildDurabilityText(ItemStack stack) {
        if (stack == null || stack.getMaxDamage() <= 0) return null;
        int max = stack.getMaxDamage();
        int remaining = max - stack.getItemDamage();
        String percent = PERCENT_FORMAT.format((double) remaining / (double) max * 100.0D);
        String type = durability.getValue();
        if (type.equals("Percentage")) return percent + "%";
        if (type.equals("Value")) return remaining + "/" + max;
        return remaining + "/" + max + " " + percent + "%";
    }

    private int durabilityColor(ItemStack stack) {
        if (colorMode.is("Custom")) {
            return customColor.getValue() & 0xFFFFFF;
        }
        int max = stack.getMaxDamage();
        double percent = (double) (max - stack.getItemDamage()) / (double) max * 100.0D;
        if (percent > 75.0D) return COLOR_GREEN;
        if (percent > 50.0D) return COLOR_ORANGE;
        if (percent > 25.0D) return COLOR_YELLOW;
        return COLOR_RED;
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : v > 1.0F ? 1.0F : v;
    }
}
