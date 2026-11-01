package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.RenderUtil;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.TextValue;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Scoreboard extends Module {

    public static Scoreboard INSTANCE;

    public final BooleanValue rounded = new BooleanValue("Rounded", false);

    public final NumberValue radius = new NumberValue("Radius", 3, 0, 8, 1);

    public final NumberValue opacity = new NumberValue("Opacity", 50, 0, 100, 1);

    public final BooleanValue customBottom = new BooleanValue("CustomBottom", false);

    public final BooleanValue remove = new BooleanValue("Remove", false);

    public final TextValue bottomText = new TextValue("BottomText", "Astra On Top");

    public final NumberValue scale = new NumberValue("Scale", 1.0, 0.5, 2.0, 0.05);

    private final NumberValue anchorX = new NumberValue("AnchorX", 0.5, 0, 1, 0.001);

    private final NumberValue anchorY = new NumberValue("AnchorY", 0.5, 0, 1, 0.001);

    private boolean positionInitialized = false;

    private static final float OUTLINE_RADIUS = 3.0F;

    private static boolean dragging = false;

    private static boolean mouseOver = false;

    private static boolean highlightVisible = false;

    private static boolean lastMiddleDown = false;

    private static int lastMouseX;

    private static int lastMouseY;

    private static float boundsLeft;

    private static float boundsTop;

    private static float boundsRight;

    private static float boundsBottom;

    private static float transX;

    private static float transY;

    private static float activeScale = 1.0F;

    public Scoreboard() {
        super("Scoreboard", Keyboard.KEY_NONE, Category.RENDER);
        INSTANCE = this;
        anchorX.setVisible(false);
        anchorY.setVisible(false);
        addValues(rounded, radius, opacity, customBottom, bottomText, scale, anchorX, anchorY, remove);
        refreshVisibility();
    }

    @Override
    public void refreshVisibility() {
        radius.setVisible(rounded.isEnabled());
        opacity.setVisible(rounded.isEnabled());
        bottomText.setVisible(customBottom.isEnabled());
    }

    public static void updateDrag(ScaledResolution sr, float vanillaLeft, float vanillaTop, float vanillaRight, float vanillaBottom) {
        Scoreboard m = INSTANCE;
        highlightVisible = false;
        mouseOver = false;

        if (m == null || !m.isToggle()) {
            dragging = false;
            lastMiddleDown = false;
            return;
        }

        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();
        float s = m.scale.getValue().floatValue();
        float boardW = (vanillaRight - vanillaLeft) * s;
        float boardH = (vanillaBottom - vanillaTop) * s;
        float vanillaCenterX = (vanillaLeft + vanillaRight) / 2.0F;
        float vanillaCenterY = (vanillaTop + vanillaBottom) / 2.0F;

        if (!m.positionInitialized) {
            m.anchorX.setValue((double) clamp01(vanillaCenterX / screenW));
            m.anchorY.setValue((double) clamp01(vanillaCenterY / screenH));
            m.positionInitialized = true;
        }

        updateGeometry(m, screenW, screenH, boardW, boardH, vanillaCenterX, vanillaCenterY, vanillaLeft, vanillaTop, vanillaRight, vanillaBottom);

        if (!(mc.currentScreen instanceof GuiChat)) {
            dragging = false;
            lastMiddleDown = false;
            return;
        }

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
                        m.anchorX.setValue((double) clamp01(m.anchorX.getValue().floatValue() + dx / screenW));
                    }
                    if (dy != 0.0F) {
                        m.anchorY.setValue((double) clamp01(m.anchorY.getValue().floatValue() + dy / screenH));
                    }
                    updateGeometry(m, screenW, screenH, boardW, boardH, vanillaCenterX, vanillaCenterY, vanillaLeft, vanillaTop, vanillaRight, vanillaBottom);
                }

                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        }

        boolean middleDown = Mouse.isButtonDown(2);

        if (middleDown && !lastMiddleDown && mouseOver) {
            m.anchorX.setValue((double) clamp01(vanillaCenterX / screenW));
            m.anchorY.setValue((double) clamp01(vanillaCenterY / screenH));
            updateGeometry(m, screenW, screenH, boardW, boardH, vanillaCenterX, vanillaCenterY, vanillaLeft, vanillaTop, vanillaRight, vanillaBottom);
        }

        lastMiddleDown = middleDown;
        highlightVisible = dragging || mouseOver;
    }

    public static void pushTransform() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(transX, transY, 0.0F);
        GlStateManager.scale(activeScale, activeScale, 1.0F);
    }

    public static void popTransform() {
        GlStateManager.popMatrix();
    }

    public static void renderHighlight() {
        if (!highlightVisible) return;
        int alpha = dragging ? 235 : 130;
        RenderUtil.drawRoundedRectOutline(boundsLeft, boundsTop, boundsRight, boundsBottom, OUTLINE_RADIUS, (alpha << 24) | 0xFFFFFF);
    }

    private static void updateGeometry(Scoreboard m, float screenW, float screenH, float boardW, float boardH,
                                       float vanillaCenterX, float vanillaCenterY,
                                       float vanillaLeft, float vanillaTop, float vanillaRight, float vanillaBottom) {
        float halfW = boardW / 2.0F;
        float halfH = boardH / 2.0F;

        float minX = halfW;
        float maxX = screenW - halfW;
        float cx = minX > maxX ? (minX + maxX) / 2.0F : Math.max(minX, Math.min(maxX, m.anchorX.getValue().floatValue() * screenW));

        float minY = halfH;
        float maxY = screenH - halfH;
        float cy = minY > maxY ? (minY + maxY) / 2.0F : Math.max(minY, Math.min(maxY, m.anchorY.getValue().floatValue() * screenH));

        activeScale = m.scale.getValue().floatValue();
        transX = cx - vanillaCenterX * activeScale;
        transY = cy - vanillaCenterY * activeScale;

        boundsLeft = transX + vanillaLeft * activeScale;
        boundsTop = transY + vanillaTop * activeScale;
        boundsRight = transX + vanillaRight * activeScale;
        boundsBottom = transY + vanillaBottom * activeScale;
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : v > 1.0F ? 1.0F : v;
    }
}
