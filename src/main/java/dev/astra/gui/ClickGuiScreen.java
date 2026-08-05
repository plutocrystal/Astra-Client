package dev.astra.gui;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.Value;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.TextValue;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ClickGuiScreen extends GuiScreen {

    private static final Color COLOR_BACKGROUND = new Color(25, 25, 25, 180);
    private static final Color COLOR_HEADER = new Color(34, 34, 34);
    private static final Color COLOR_TEXT_FIELD_BG = new Color(34, 34, 34);
    private static final Color COLOR_TEXT_FIELD_FG = new Color(45, 45, 45, 200);
    private static final Color COLOR_SLIDER_BG = new Color(34, 34, 34);
    private static final Color COLOR_SEPARATOR = new Color(34, 34, 34);
    private static final Color COLOR_TEXT_PRIMARY = new Color(200, 200, 200);
    private static final Color COLOR_TEXT_SECONDARY = new Color(180, 180, 180);
    private static final Color COLOR_TEXT_HOVER = new Color(220, 220, 220);
    private static final Color COLOR_TEXT_PLACEHOLDER = new Color(120, 120, 120);
    private static final Color COLOR_ENABLED = new Color(0, 180, 0);
    private static final Color COLOR_DISABLED = new Color(180, 0, 0);
    private static final Color COLOR_INDICATOR_WHITE = Color.WHITE;
    private static final Color COLOR_INDICATOR_BLACK = Color.BLACK;
    private static final Color COLOR_WIN11_CLOSE_HOVER = new Color(196, 43, 28);
    private static final Color COLOR_WIN11_BTN_HOVER = new Color(255, 255, 255, 15);

    private static final float WIDTH = 500.0f;
    private static final float HEIGHT = 300.0f;
    private static final float HEADER_HEIGHT = 17.0f;
    private static final float CATEGORY_OFFSET_X = 90.0f;
    private static final float MODULE_LIST_WIDTH = 90.0f;
    private static final float VALUE_AREA_OFFSET_X = 100.0f;
    private static final float TEXT_FIELD_WIDTH = 100.0f;
    private static final float SLIDER_WIDTH = 98.0f;
    private static final float SLIDER_HEIGHT = 10.0f;
    private static final float COLOR_PICKER_WIDTH = 100.0f;
    private static final float COLOR_PICKER_HEIGHT = 50.0f;
    private static final float HUE_SLIDER_HEIGHT = 5.0f;
    private static final float ALPHA_SLIDER_HEIGHT = 5.0f;
    private static final float COLOR_PREVIEW_SIZE = 20.0f;
    private static final float SCROLL_SPEED = 0.1f;
    private static final float ANIMATION_SPEED_GUI = 8.0f;
    private static final float MAX_DELTA_TIME = 0.1f;
    private static final float WIN11_BUTTON_WIDTH = 35.0f;

    private boolean dragging = false;
    private boolean waitingForKey = false;
    private boolean draggingSlider = false;
    private boolean isMaximized = false;
    private float savedPosX, savedPosY, savedWidth, savedHeight;

    private float guiOpenAnimation = 0.0f;
    private long lastAnimationTime = System.currentTimeMillis();
    private float draggingX;
    private float draggingY;
    private float posX = 150.0f;
    private float posY = 80.0f;
    private float currentWidth = WIDTH;
    private float currentHeight = HEIGHT;
    private float valueScroll = 0.0f;
    private float moduleScroll = 0.0f;

    private Module selectedModule;
    private Category selectedCategory = null;
    private NumberValue currentDraggingSlider = null;

    private final HashMap<TextValue, GuiTextField> textFieldMap = new HashMap<>();
    private final Map<ColorValue, ColorPickerState> colorPickerStates = new HashMap<>();
    private final HashMap<NumberValue, Float> numberSettingMap = new HashMap<>();

    private float categoryLineAnimation = 0.0f;
    private float lastCategoryLineTargetX = -1337.0f;

    private static final File CONFIG_FILE = new File("astra_clickgui.cfg");

    private static class ColorPickerState {
        float pickerX, pickerY, hueSliderY, alphaSliderY;
        boolean draggingHue, draggingColor, draggingAlpha;
        float hue, saturation, brightness, alpha;
    }

    public enum GuiEvent {
        DRAW, CLICK, RELEASE
    }

    public ClickGuiScreen() {
        loadState();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());
        updateAnimations();
        handle(mouseX, mouseY, -1, GuiEvent.DRAW);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handle(mouseX, mouseY, mouseButton, GuiEvent.CLICK);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        handle(mouseX, mouseY, state, GuiEvent.RELEASE);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (waitingForKey) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                selectedModule.setKeycode(0);
            } else {
                selectedModule.setKeycode(keyCode);
            }
            waitingForKey = false;
            return;
        }

        for (GuiTextField tf : textFieldMap.values()) {
            tf.textboxKeyTyped(typedChar, keyCode);
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void onGuiClosed() {
        saveState();
        super.onGuiClosed();
        dev.astra.module.impl.render.ClickGui clickGui = dev.astra.Main.moduleManager.getModuleByClass(dev.astra.module.impl.render.ClickGui.class);
        if (clickGui != null && clickGui.isToggle()) {
            clickGui.setToggle(false);
        }
        textFieldMap.clear();
        colorPickerStates.clear();
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.max(0.0f, Math.min(MAX_DELTA_TIME, (currentTime - lastAnimationTime) / 1000.0f));
        lastAnimationTime = currentTime;

        float targetGuiAnimation = 1.0f;
        guiOpenAnimation = Math.max(0.0f, Math.min(1.0f, (float) animate(targetGuiAnimation, guiOpenAnimation, deltaTime * ANIMATION_SPEED_GUI)));
    }

    private Color getGlobalColor() {
        return new Color(81, 149, 219);
    }

    public void handle(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (event == GuiEvent.RELEASE) {
            this.dragging = false;
            this.draggingSlider = false;
            this.currentDraggingSlider = null;
            for (ColorPickerState state : colorPickerStates.values()) {
                state.draggingHue = false;
                state.draggingColor = false;
                state.draggingAlpha = false;
            }
        }

        if (event == GuiEvent.CLICK && handleWin11Buttons(mouseX, mouseY, mouseButton)) {
            return;
        }

        handleDragging(mouseX, mouseY, mouseButton, event);

        if (event == GuiEvent.DRAW) {
            renderMainBackground();
            renderHeader(mouseX, mouseY);
        }

        renderCategories(mouseX, mouseY, mouseButton, event);
        renderModuleList(mouseX, mouseY, mouseButton, event);
        renderValueSettings(mouseX, mouseY, mouseButton, event);

        if (event == GuiEvent.DRAW) {
            updateColorPickerDrag(mouseX, mouseY);
        }
    }

    private boolean handleWin11Buttons(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false;

        float closeX = posX + currentWidth - WIN11_BUTTON_WIDTH;
        float maxX = posX + currentWidth - WIN11_BUTTON_WIDTH * 2;
        float minX = posX + currentWidth - WIN11_BUTTON_WIDTH * 3;

        if (isHovered(mouseX, mouseY, closeX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT)) {
            mc.displayGuiScreen(null);
            return true;
        }
        if (isHovered(mouseX, mouseY, maxX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT)) {
            toggleMaximize();
            return true;
        }
        if (isHovered(mouseX, mouseY, minX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT)) {
            mc.displayGuiScreen(null);
            return true;
        }
        return false;
    }

    private void toggleMaximize() {
        if (isMaximized) {
            posX = savedPosX;
            posY = savedPosY;
            currentWidth = savedWidth;
            currentHeight = savedHeight;
            isMaximized = false;
        } else {
            savedPosX = posX;
            savedPosY = posY;
            savedWidth = currentWidth;
            savedHeight = currentHeight;
            posX = 0;
            posY = 0;
            currentWidth = this.width;
            currentHeight = this.height;
            isMaximized = true;
        }
    }

    private void handleDragging(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (event == GuiEvent.CLICK) {
            float buttonsStartX = posX + currentWidth - WIN11_BUTTON_WIDTH * 3;
            if (isHovered(mouseX, mouseY, posX, posY, currentWidth, HEADER_HEIGHT) && mouseButton == 0 && mouseX < buttonsStartX) {
                dragging = true;
                draggingX = mouseX - posX;
                draggingY = mouseY - posY;
            }
        }

        if (event == GuiEvent.DRAW && dragging) {
            if (Mouse.isButtonDown(0)) {
                posX = mouseX - draggingX;
                posY = mouseY - draggingY;
            } else {
                dragging = false;
            }
        }
    }

    private void renderMainBackground() {
        drawRect(posX, posY, currentWidth, currentHeight, COLOR_BACKGROUND.getRGB());
    }

    private void renderHeader(int mouseX, int mouseY) {
        drawRect(posX, posY, currentWidth, HEADER_HEIGHT, COLOR_HEADER.getRGB());
        mc.fontRendererObj.drawStringWithShadow("ASTRA", posX + 5.0f, posY + 6.0f, COLOR_TEXT_PRIMARY.getRGB());

        drawRect(posX + CATEGORY_OFFSET_X, posY + 0.5f, 2.0f, currentHeight, COLOR_SEPARATOR.getRGB());
        drawRect(posX + CATEGORY_OFFSET_X, posY + 40.0f, currentWidth - CATEGORY_OFFSET_X + 0.5f, 2.0f, COLOR_SEPARATOR.getRGB());

        renderWin11Buttons(mouseX, mouseY);
    }

    private void renderWin11Buttons(int mouseX, int mouseY) {
        float closeX = posX + currentWidth - WIN11_BUTTON_WIDTH;
        float maxX = posX + currentWidth - WIN11_BUTTON_WIDTH * 2;
        float minX = posX + currentWidth - WIN11_BUTTON_WIDTH * 3;

        boolean closeHover = isHovered(mouseX, mouseY, closeX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT);
        boolean maxHover = isHovered(mouseX, mouseY, maxX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT);
        boolean minHover = isHovered(mouseX, mouseY, minX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT);

        if (minHover) drawRect(minX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT, COLOR_WIN11_BTN_HOVER.getRGB());
        if (maxHover) drawRect(maxX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT, COLOR_WIN11_BTN_HOVER.getRGB());
        if (closeHover) drawRect(closeX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT, COLOR_WIN11_CLOSE_HOVER.getRGB());

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(1.2f);

        float minCenterX = minX + WIN11_BUTTON_WIDTH / 2.0f;
        float maxCenterX = maxX + WIN11_BUTTON_WIDTH / 2.0f;
        float closeCenterX = closeX + WIN11_BUTTON_WIDTH / 2.0f;
        float centerY = posY + HEADER_HEIGHT / 2.0f;

        float iconR = 0.8f, iconG = 0.8f, iconB = 0.8f;
        if (minHover) { iconR = 1.0f; iconG = 1.0f; iconB = 1.0f; }
        GL11.glColor4f(iconR, iconG, iconB, 1.0f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(minCenterX - 5.0f, posY + HEADER_HEIGHT - 5.0f);
        GL11.glVertex2f(minCenterX + 5.0f, posY + HEADER_HEIGHT - 5.0f);
        GL11.glEnd();

        if (maxHover) { iconR = 1.0f; iconG = 1.0f; iconB = 1.0f; } else { iconR = 0.8f; iconG = 0.8f; iconB = 0.8f; }
        GL11.glColor4f(iconR, iconG, iconB, 1.0f);
        if (!isMaximized) {
            float s = 4.5f;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(maxCenterX - s, centerY - s);
            GL11.glVertex2f(maxCenterX + s, centerY - s);
            GL11.glVertex2f(maxCenterX + s, centerY + s);
            GL11.glVertex2f(maxCenterX - s, centerY + s);
            GL11.glEnd();
        } else {
            float s = 3.5f, d = 2.5f;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(maxCenterX - s + d, centerY - s);
            GL11.glVertex2f(maxCenterX + s + d, centerY - s);
            GL11.glVertex2f(maxCenterX + s + d, centerY + s);
            GL11.glVertex2f(maxCenterX - s + d, centerY + s);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(maxCenterX - s, centerY - s + d);
            GL11.glVertex2f(maxCenterX + s, centerY - s + d);
            GL11.glVertex2f(maxCenterX + s, centerY + s + d);
            GL11.glVertex2f(maxCenterX - s, centerY + s + d);
            GL11.glEnd();
        }

        if (closeHover) { iconR = 1.0f; iconG = 1.0f; iconB = 1.0f; } else { iconR = 0.8f; iconG = 0.8f; iconB = 0.8f; }
        GL11.glColor4f(iconR, iconG, iconB, 1.0f);
        float cs = 4.0f;
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(closeCenterX - cs, centerY - cs);
        GL11.glVertex2f(closeCenterX + cs, centerY + cs);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(closeCenterX + cs, centerY - cs);
        GL11.glVertex2f(closeCenterX - cs, centerY + cs);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void renderCategories(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        float categoryX = posX + CATEGORY_OFFSET_X + 15.0f;
        float targetLineX = -1337.0f;

        for (Category category : Category.values()) {
            String displayName = category.name().charAt(0) + category.name().substring(1).toLowerCase();
            float categoryWidth = mc.fontRendererObj.getStringWidth(displayName);
            float categoryHeight = mc.fontRendererObj.FONT_HEIGHT;

            if (event == GuiEvent.DRAW) {
                boolean isSelected = category == this.selectedCategory;
                boolean isHovered = isHovered(mouseX, mouseY, categoryX, posY + 20.0f, categoryWidth, categoryHeight);

                int textColor = isSelected ? getGlobalColor().getRGB() : (isHovered ? COLOR_TEXT_HOVER.getRGB() : COLOR_TEXT_SECONDARY.getRGB());
                mc.fontRendererObj.drawStringWithShadow(displayName, categoryX, posY + 25.0f, textColor);

                if (isSelected) targetLineX = categoryX;
            } else if (event == GuiEvent.CLICK) {
                if (isHovered(mouseX, mouseY, categoryX, posY + 20.0f, categoryWidth, categoryHeight)) {
                    selectedCategory = selectedCategory == category ? null : category;
                    selectedModule = null;
                    moduleScroll = 0.0f;
                    valueScroll = 0.0f;
                    colorPickerStates.clear();
                }
            }
            categoryX += categoryWidth + 15.0f;
        }

        if (event == GuiEvent.DRAW && selectedCategory != null && targetLineX != -1337.0f) {
            categoryLineAnimation = (float) animate(targetLineX, categoryLineAnimation, 0.2);
            float lineWidth = mc.fontRendererObj.getStringWidth(selectedCategory.name().charAt(0) + selectedCategory.name().substring(1).toLowerCase()) - 0.5f;
            float lineY = posY + 25.0f + mc.fontRendererObj.FONT_HEIGHT + 2.0f;
            drawRect(categoryLineAnimation, lineY, lineWidth, 2.0f, getGlobalColor().getRGB());
        }
    }

    private void renderModuleList(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (event == GuiEvent.DRAW) {
            scissorStart(posX, posY + 16.5f, MODULE_LIST_WIDTH, currentHeight - 16.5f);
        }

        if (isHovered(mouseX, mouseY, posX, posY + 16.5f, MODULE_LIST_WIDTH, currentHeight - 16.5f) && event == GuiEvent.DRAW) {
            moduleScroll = Math.min(0.0f, moduleScroll + Mouse.getDWheel() * SCROLL_SPEED);
        }

        float moduleY = posY + 25.0f + moduleScroll;
        List<Module> modules = getCurrentModuleList();

        for (Module module : modules) {
            float moduleHeight = mc.fontRendererObj.FONT_HEIGHT;

            if (event == GuiEvent.DRAW) {
                float drawX = posX + 8.0f;
                if (module == selectedModule) {
                    int arrowColor = module.isToggle() ? getGlobalColor().getRGB() : COLOR_TEXT_PRIMARY.getRGB();
                    mc.fontRendererObj.drawStringWithShadow(">", drawX, moduleY, arrowColor);
                    drawX += mc.fontRendererObj.getStringWidth("> ") + 2.0f;
                }
                int nameColor = module.isToggle() ? getGlobalColor().getRGB() : COLOR_TEXT_PRIMARY.getRGB();
                mc.fontRendererObj.drawStringWithShadow(module.getName(), drawX, moduleY, nameColor);
            } else if (event == GuiEvent.CLICK) {
                float nameWidth = mc.fontRendererObj.getStringWidth(module.getName());
                if (isHovered(mouseX, mouseY, posX + 8.0f, moduleY, nameWidth, moduleHeight)) {
                    if (mouseButton == 0) {
                        if (module instanceof dev.astra.module.impl.render.ClickGui) {
                            mc.displayGuiScreen(null);
                            return;
                        }
                        module.toggle();
                    } else if (mouseButton == 1) {
                        selectedModule = module;
                        valueScroll = 0.0f;
                        colorPickerStates.clear();
                    }
                }
            }
            moduleY += moduleHeight + 4.0f;
        }

        if (event == GuiEvent.DRAW) {
            scissorEnd();
        }
    }

    private void renderValueSettings(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (selectedModule == null) return;

        float initialValueY = posY + 40.0f;
        float currentY = initialValueY + 8.0f;

        if (event == GuiEvent.DRAW) {
            mc.fontRendererObj.drawStringWithShadow(selectedModule.getName() + ":", posX + VALUE_AREA_OFFSET_X, currentY, COLOR_TEXT_PRIMARY.getRGB());
        }

        currentY += mc.fontRendererObj.FONT_HEIGHT + 3.0f;
        renderKeyAndHideSettings(mouseX, mouseY, mouseButton, event, currentY);

        float headerHeight = (initialValueY + 8.0f) - (posY + 40.0f) + mc.fontRendererObj.FONT_HEIGHT + 3.0f + mc.fontRendererObj.FONT_HEIGHT + 10.0f;

        if (isHovered(mouseX, mouseY, posX + CATEGORY_OFFSET_X + 1.5f, initialValueY + 1.5f + 1.0f, currentWidth - (CATEGORY_OFFSET_X + 1.5f), currentHeight - (40.0f + 1.5f)) && event == GuiEvent.DRAW) {
            float scrollDelta = Mouse.getDWheel() * SCROLL_SPEED;
            if (scrollDelta != 0) {
                valueScroll = Math.min(0.0f, valueScroll + scrollDelta);
            }
        }

        if (event == GuiEvent.DRAW) {
            scissorStart(posX + CATEGORY_OFFSET_X + 1.5f + 0.5f, posY + 30.0f + headerHeight + 1.5f + 1.0f, currentWidth - (CATEGORY_OFFSET_X + 1.5f), currentHeight - (31.0f + headerHeight + 1.5f));
        }

        currentY = initialValueY - 4.0f + headerHeight + valueScroll;

        for (Value<?> value : selectedModule.getValues()) {
            currentY = renderProperty(mouseX, mouseY, mouseButton, event, currentY, value);
        }

        if (event == GuiEvent.DRAW) {
            scissorEnd();
        }
    }

    private void renderKeyAndHideSettings(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY) {
        String keyName = selectedModule.getKeycode() == 0 ? "None" : Keyboard.getKeyName(selectedModule.getKeycode());
        float keyWidth = mc.fontRendererObj.getStringWidth("Key: " + keyName);

        boolean isKeyHovered = isHovered(mouseX, mouseY, posX + VALUE_AREA_OFFSET_X, currentY + 1.0f, keyWidth, mc.fontRendererObj.FONT_HEIGHT);

        if (event == GuiEvent.DRAW) {
            int hideTextColor = new Color(150, 150, 150).getRGB();
            if (waitingForKey) {
                mc.fontRendererObj.drawStringWithShadow("Key: ...", posX + VALUE_AREA_OFFSET_X, currentY + 1.0f, getGlobalColor().getRGB());
            } else {
                mc.fontRendererObj.drawStringWithShadow("Key: " + keyName, posX + VALUE_AREA_OFFSET_X, currentY + 1.0f, hideTextColor);
            }
        } else if (event == GuiEvent.CLICK) {
            if (isKeyHovered && mouseButton == 0) {
                waitingForKey = !waitingForKey;
            }
        }
    }

    private float renderProperty(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY, Value<?> property) {
        if (property instanceof BooleanValue) {
            return renderBoolValue(mouseX, mouseY, mouseButton, event, currentY, (BooleanValue) property);
        } else if (property instanceof TextValue) {
            return renderTextValue(mouseX, mouseY, mouseButton, event, currentY, (TextValue) property);
        } else if (property instanceof NumberValue) {
            return renderNumValue(mouseX, mouseY, mouseButton, event, currentY, (NumberValue) property);
        } else if (property instanceof ModeValue) {
            return renderModeValue(mouseX, mouseY, mouseButton, event, currentY, (ModeValue) property);
        } else if (property instanceof ColorValue) {
            return renderColorValue(mouseX, mouseY, mouseButton, event, currentY, (ColorValue) property);
        }
        return currentY;
    }

    private float renderBoolValue(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY, BooleanValue boolValue) {
        String valueText = boolValue.getValue() ? "true" : "false";
        float fullWidth = mc.fontRendererObj.getStringWidth(boolValue.getName() + ": " + valueText);

        if (event == GuiEvent.DRAW) {
            mc.fontRendererObj.drawStringWithShadow(boolValue.getName() + ": ", posX + VALUE_AREA_OFFSET_X, currentY, COLOR_TEXT_PRIMARY.getRGB());
            int valueColor = boolValue.getValue() ? COLOR_ENABLED.getRGB() : COLOR_DISABLED.getRGB();
            mc.fontRendererObj.drawStringWithShadow(valueText, posX + VALUE_AREA_OFFSET_X + mc.fontRendererObj.getStringWidth(boolValue.getName() + ": "), currentY, valueColor);
        } else if (event == GuiEvent.CLICK) {
            if (isHovered(mouseX, mouseY, posX + VALUE_AREA_OFFSET_X, currentY, fullWidth, mc.fontRendererObj.FONT_HEIGHT)) {
                boolValue.setValue(!boolValue.getValue());
            }
        }
        return currentY + mc.fontRendererObj.FONT_HEIGHT + 4.0f;
    }

        private float renderTextValue(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY, TextValue textValue) {
        float textFieldX = posX + VALUE_AREA_OFFSET_X + mc.fontRendererObj.getStringWidth(textValue.getName() + ": ");
        float textFieldY = currentY - 2.5f;
        float textFieldHeight = mc.fontRendererObj.FONT_HEIGHT + 2.0f;
        int tfWidth = (int) (TEXT_FIELD_WIDTH - 8.0f);
        int tfHeight = (int) textFieldHeight;

        GuiTextField textField = textFieldMap.computeIfAbsent(textValue, k -> {
            GuiTextField tf = new GuiTextField(0, mc.fontRendererObj, (int) textFieldX, (int) textFieldY, tfWidth, tfHeight);
            tf.setText(textValue.getValue());
            tf.setEnableBackgroundDrawing(false);
            return tf;
        });

        if (event == GuiEvent.DRAW) {
            mc.fontRendererObj.drawStringWithShadow(textValue.getName() + ": ", posX + VALUE_AREA_OFFSET_X, currentY, COLOR_TEXT_PRIMARY.getRGB());
            drawRect(textFieldX - 1.0f, textFieldY, TEXT_FIELD_WIDTH + 2.0f, textFieldHeight, COLOR_TEXT_FIELD_BG.getRGB());
            drawRect(textFieldX, textFieldY + 1.0f, TEXT_FIELD_WIDTH, textFieldHeight - 2.0f, COLOR_TEXT_FIELD_FG.getRGB());

            textField.xPosition = (int) (textFieldX + 4.0f);
            textField.yPosition = (int) (textFieldY + (textFieldHeight - mc.fontRendererObj.FONT_HEIGHT) / 2.0f + 1.0f);
            textField.updateCursorCounter();

            String displayText = textField.getText();
            if (displayText.isEmpty() && !textField.isFocused()) {
                mc.fontRendererObj.drawStringWithShadow("Enter text...", textFieldX + 4.0f, textFieldY + (textFieldHeight - mc.fontRendererObj.FONT_HEIGHT) / 2.0f + 1.0f, COLOR_TEXT_PLACEHOLDER.getRGB());
            } else {
                textField.drawTextBox();
            }
            textValue.setValue(textField.getText());
        } else if (event == GuiEvent.CLICK) {
            textField.xPosition = (int) textFieldX;
            textField.yPosition = (int) (textFieldY + (textFieldHeight - mc.fontRendererObj.FONT_HEIGHT) / 2.0f);
            textField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return currentY + mc.fontRendererObj.FONT_HEIGHT + 4.0f;
    }

    private float renderNumValue(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY, NumberValue numValue) {
        String nameText = numValue.getName() + ": ";
        float nameWidth = mc.fontRendererObj.getStringWidth(nameText);
        float sliderX = posX + VALUE_AREA_OFFSET_X + nameWidth;
        float sliderY = currentY + (mc.fontRendererObj.FONT_HEIGHT - SLIDER_HEIGHT) / 2.0f;

        double min = numValue.getMin(), max = numValue.getMax(), increment = numValue.getIncrement(), currentVal = numValue.getValue();

        if (event == GuiEvent.DRAW) {
            mc.fontRendererObj.drawStringWithShadow(nameText, posX + VALUE_AREA_OFFSET_X, currentY, COLOR_TEXT_PRIMARY.getRGB());

            double range = max - min;
            double targetLength = range == 0 ? 0 : Math.max(0.0, Math.min(SLIDER_WIDTH, (currentVal - min) / range * SLIDER_WIDTH));
            double currentLength = numberSettingMap.getOrDefault(numValue, (float) targetLength);

            if (draggingSlider && currentDraggingSlider == numValue) {
                currentLength = targetLength;
            } else {
                currentLength = animate(targetLength, currentLength, 0.2);
            }

            numberSettingMap.put(numValue, (float) currentLength);

            drawRect(sliderX, sliderY, SLIDER_WIDTH, SLIDER_HEIGHT, COLOR_SLIDER_BG.getRGB());
            drawRect(sliderX, sliderY, (float) currentLength, SLIDER_HEIGHT, getGlobalColor().getRGB());

            String valueStr = String.valueOf(Math.round(currentVal * 100.0) / 100.0);
            float valueStrWidth = mc.fontRendererObj.getStringWidth(valueStr);
            float textX = sliderX + SLIDER_WIDTH / 2.0f - valueStrWidth / 2.0f;
            float textY = sliderY + SLIDER_HEIGHT / 2.0f - mc.fontRendererObj.FONT_HEIGHT / 2.0f;

            mc.fontRendererObj.drawStringWithShadow(valueStr, textX, textY, COLOR_TEXT_PRIMARY.getRGB());

            if (draggingSlider && currentDraggingSlider == numValue && Mouse.isButtonDown(0)) {
                updateSliderValue(mouseX, sliderX, SLIDER_WIDTH, min, max, increment, numValue);
            }
        } else if (event == GuiEvent.CLICK) {
            if (mouseButton == 0 && isHovered(mouseX, mouseY, sliderX, sliderY - 2.0f, SLIDER_WIDTH, SLIDER_HEIGHT + 4.0f)) {
                draggingSlider = true;
                currentDraggingSlider = numValue;
                updateSliderValue(mouseX, sliderX, SLIDER_WIDTH, min, max, increment, numValue);
            }
        }
        return currentY + Math.max(mc.fontRendererObj.FONT_HEIGHT, SLIDER_HEIGHT) + 4.0f;
    }

    private float renderModeValue(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY, ModeValue modeValue) {
        float startX = posX + VALUE_AREA_OFFSET_X;
        float tempY = currentY;
        float currentX = startX;

        String nameText = modeValue.getName() + ": ";
        float nameWidth = mc.fontRendererObj.getStringWidth(nameText);

        if (event == GuiEvent.DRAW) {
            mc.fontRendererObj.drawStringWithShadow(nameText, currentX, tempY, COLOR_TEXT_PRIMARY.getRGB());
        }

        currentX += nameWidth;
        float rightBoundary = posX + currentWidth - 10.0f;
        float spacing = 10.0f;

        for (String mode : modeValue.getModes()) {
            float modeWidth = mc.fontRendererObj.getStringWidth(mode);

            if (currentX + modeWidth > rightBoundary && currentX > startX) {
                currentX = startX;
                tempY += mc.fontRendererObj.FONT_HEIGHT + 4.0f;
            }

            boolean isSelected = modeValue.getValue().equals(mode);
            boolean isHovered = isHovered(mouseX, mouseY, currentX - 2.0f, tempY, modeWidth + 4.0f, mc.fontRendererObj.FONT_HEIGHT);

            if (event == GuiEvent.DRAW) {
                int color = isSelected ? getGlobalColor().getRGB() : (isHovered ? COLOR_TEXT_HOVER.getRGB() : COLOR_TEXT_SECONDARY.getRGB());
                mc.fontRendererObj.drawStringWithShadow(mode, currentX, tempY, color);
            } else if (event == GuiEvent.CLICK) {
                if (isHovered && mouseButton == 0) {
                    modeValue.setMode(mode);
                }
            }
            currentX += modeWidth + spacing;
        }
        return tempY + mc.fontRendererObj.FONT_HEIGHT + 4.0f;
    }

    private float renderColorValue(int mouseX, int mouseY, int mouseButton, GuiEvent event, float currentY, ColorValue colorValue) {
        float pickerX = posX + VALUE_AREA_OFFSET_X;
        ColorPickerState state = getColorState(colorValue);

        state.pickerX = pickerX;
        state.pickerY = currentY + mc.fontRendererObj.FONT_HEIGHT;
        state.hueSliderY = state.pickerY + COLOR_PICKER_HEIGHT + 5.0f;
        state.alphaSliderY = state.hueSliderY + HUE_SLIDER_HEIGHT + 5.0f;

        if (event == GuiEvent.DRAW) {
            mc.fontRendererObj.drawStringWithShadow(colorValue.getName() + ": ", posX + VALUE_AREA_OFFSET_X, currentY, COLOR_TEXT_PRIMARY.getRGB());

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            Color hueColor = Color.getHSBColor(state.hue, 1.0f, 1.0f);
            drawRect(state.pickerX, state.pickerY, COLOR_PICKER_WIDTH, COLOR_PICKER_HEIGHT, hueColor.getRGB());

            renderColorPickerGradients(state);
            renderHueSlider(state);
            renderAlphaSlider(state, colorValue);
            renderColorPreview(state, colorValue);
            renderColorIndicators(state);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
        } else if (event == GuiEvent.CLICK) {
            handleColorPickerClick(mouseX, mouseY, mouseButton, state, colorValue);
        }

        return currentY + mc.fontRendererObj.FONT_HEIGHT + 4.0f + COLOR_PICKER_HEIGHT + 5.0f + HUE_SLIDER_HEIGHT + 5.0f + ALPHA_SLIDER_HEIGHT + 4.0f;
    }

    private ColorPickerState getColorState(ColorValue prop) {
        return colorPickerStates.computeIfAbsent(prop, k -> {
            ColorPickerState s = new ColorPickerState();
            Color c = new Color(prop.getValue(), true);
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            s.hue = hsb[0]; s.saturation = hsb[1]; s.brightness = hsb[2]; s.alpha = c.getAlpha() / 255.0f;
            return s;
        });
    }

    private void renderColorPickerGradients(ColorPickerState state) {
        for (int x = 0; x < COLOR_PICKER_WIDTH; x++) {
            float saturation = x / COLOR_PICKER_WIDTH;
            Color whiteGradient = new Color(255, 255, 255, (int) (255 * (1.0f - saturation)));
            drawRect(state.pickerX + x, state.pickerY, 1.0f, COLOR_PICKER_HEIGHT, whiteGradient.getRGB());
        }
        for (int y = 0; y < COLOR_PICKER_HEIGHT; y++) {
            float brightness = 1.0f - (y / COLOR_PICKER_HEIGHT);
            Color blackGradient = new Color(0, 0, 0, (int) (255 * (1.0f - brightness)));
            drawRect(state.pickerX, state.pickerY + y, COLOR_PICKER_WIDTH, 1.0f, blackGradient.getRGB());
        }
    }

    private void renderHueSlider(ColorPickerState state) {
        drawRect(state.pickerX, state.hueSliderY, COLOR_PICKER_WIDTH, HUE_SLIDER_HEIGHT, COLOR_INDICATOR_BLACK.getRGB());
        for (float x = 0; x < COLOR_PICKER_WIDTH; x++) {
            float hue = x / COLOR_PICKER_WIDTH;
            Color c = Color.getHSBColor(hue, 1.0f, 1.0f);
            drawRect(state.pickerX + x, state.hueSliderY + 1.0f, 1.0f, HUE_SLIDER_HEIGHT - 2.0f, c.getRGB());
        }
        float huePos = state.pickerX + (state.hue * COLOR_PICKER_WIDTH);
        drawRect(huePos - 2.0f, state.hueSliderY - 2.0f, 4.0f, HUE_SLIDER_HEIGHT + 4.0f, COLOR_INDICATOR_WHITE.getRGB());
        drawRect(huePos - 1.0f, state.hueSliderY, 2.0f, HUE_SLIDER_HEIGHT, COLOR_INDICATOR_BLACK.getRGB());
    }

    private void renderAlphaSlider(ColorPickerState state, ColorValue colorValue) {
        drawRect(state.pickerX, state.alphaSliderY, COLOR_PICKER_WIDTH, ALPHA_SLIDER_HEIGHT, COLOR_INDICATOR_WHITE.getRGB());
        int rgb = Color.HSBtoRGB(state.hue, state.saturation, state.brightness);
        Color currentColor = new Color(rgb);
        for (float x = 0; x < COLOR_PICKER_WIDTH; x++) {
            float alpha = x / COLOR_PICKER_WIDTH;
            Color c = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), (int)(alpha * 255));
            drawRect(state.pickerX + x, state.alphaSliderY + 1.0f, 1.0f, ALPHA_SLIDER_HEIGHT - 2.0f, c.getRGB());
        }
        float alphaPos = state.pickerX + (state.alpha * COLOR_PICKER_WIDTH);
        drawRect(alphaPos - 2.0f, state.alphaSliderY - 2.0f, 4.0f, ALPHA_SLIDER_HEIGHT + 4.0f, COLOR_INDICATOR_WHITE.getRGB());
        drawRect(alphaPos - 1.0f, state.alphaSliderY, 2.0f, ALPHA_SLIDER_HEIGHT, COLOR_INDICATOR_BLACK.getRGB());
    }

    private void renderColorPreview(ColorPickerState state, ColorValue colorValue) {
        float previewX = state.pickerX + COLOR_PICKER_WIDTH + 5.0f;
        float previewY = state.pickerY;
        drawRect(previewX, previewY, COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE, COLOR_INDICATOR_WHITE.getRGB());
        drawRect(previewX + 1, previewY + 1, COLOR_PREVIEW_SIZE - 2, COLOR_PREVIEW_SIZE - 2, colorValue.getValue());
    }

    private void renderColorIndicators(ColorPickerState state) {
        float indicatorX = state.pickerX + state.saturation * COLOR_PICKER_WIDTH;
        float indicatorY = state.pickerY + (1.0f - state.brightness) * COLOR_PICKER_HEIGHT;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        float r = 3.0f;
        for (int i = 0; i <= 360; i += 30) {
            double rad = Math.toRadians(i);
            GL11.glVertex2f(indicatorX + (float) Math.cos(rad) * r, indicatorY + (float) Math.sin(rad) * r);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void handleColorPickerClick(int mouseX, int mouseY, int mouseButton, ColorPickerState state, ColorValue colorValue) {
        if (mouseButton != 0) return;
        if (isHovered(mouseX, mouseY, state.pickerX, state.pickerY, COLOR_PICKER_WIDTH, COLOR_PICKER_HEIGHT)) {
            state.draggingColor = true;
            state.saturation = Math.max(0.0f, Math.min(1.0f, (mouseX - state.pickerX) / COLOR_PICKER_WIDTH));
            state.brightness = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - state.pickerY) / COLOR_PICKER_HEIGHT));
            updateColorFromState(state, colorValue);
        } else if (isHovered(mouseX, mouseY, state.pickerX, state.hueSliderY, COLOR_PICKER_WIDTH, HUE_SLIDER_HEIGHT)) {
            state.draggingHue = true;
            state.hue = Math.max(0.0f, Math.min(1.0f, (mouseX - state.pickerX) / COLOR_PICKER_WIDTH));
            updateColorFromState(state, colorValue);
        } else if (isHovered(mouseX, mouseY, state.pickerX, state.alphaSliderY, COLOR_PICKER_WIDTH, ALPHA_SLIDER_HEIGHT)) {
            state.draggingAlpha = true;
            state.alpha = Math.max(0.0f, Math.min(1.0f, (mouseX - state.pickerX) / COLOR_PICKER_WIDTH));
            updateColorFromState(state, colorValue);
        }
    }

    private void updateColorPickerDrag(int mouseX, int mouseY) {
        for (Map.Entry<ColorValue, ColorPickerState> entry : colorPickerStates.entrySet()) {
            ColorValue colorValue = entry.getKey();
            ColorPickerState state = entry.getValue();
            if (state.draggingColor) {
                state.saturation = Math.max(0.0f, Math.min(1.0f, (mouseX - state.pickerX) / COLOR_PICKER_WIDTH));
                state.brightness = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - state.pickerY) / COLOR_PICKER_HEIGHT));
                updateColorFromState(state, colorValue);
            }
            if (state.draggingHue) {
                state.hue = Math.max(0.0f, Math.min(1.0f, (mouseX - state.pickerX) / COLOR_PICKER_WIDTH));
                updateColorFromState(state, colorValue);
            }
            if (state.draggingAlpha) {
                state.alpha = Math.max(0.0f, Math.min(1.0f, (mouseX - state.pickerX) / COLOR_PICKER_WIDTH));
                updateColorFromState(state, colorValue);
            }
        }
    }

    private void updateColorFromState(ColorPickerState state, ColorValue colorValue) {
        int rgb = Color.HSBtoRGB(state.hue, state.saturation, state.brightness);
        Color c = new Color(rgb);
        int rgba = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(state.alpha * 255)).getRGB();
        colorValue.setValue(rgba);
    }

    private void updateSliderValue(int mouseX, float sliderX, float sliderWidth, double min, double max, double increment, NumberValue numValue) {
        double percent = (mouseX - sliderX) / sliderWidth;
        percent = Math.max(0.0, Math.min(1.0, percent));
        double val = min + percent * (max - min);
        if (increment > 0) val = Math.round(val / increment) * increment;
        val = Math.max(min, Math.min(max, val));
        numValue.setValue(val);
    }

    private List<Module> getCurrentModuleList() {
        List<Module> modules = new ArrayList<>();
        if (selectedCategory == null) {
            modules.addAll(Main.moduleManager.getModules());
        } else {
            for (Module mod : Main.moduleManager.getModules()) {
                if (mod.getCategory() == selectedCategory) modules.add(mod);
            }
        }
        modules.sort(Comparator.comparing(Module::getName));
        return modules;
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void scissorStart(float x, float y, float width, float height) {
        if (width <= 0 || height <= 0) return;
        
        ScaledResolution sr = new ScaledResolution(mc);
        double scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) ((sr.getScaledHeight() - y - height) * scale),
                (int) (width * scale), (int) (height * scale));
    }

    private void scissorEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawRect(float x, float y, float width, float height, int color) {
        GuiScreen.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    private double animate(double target, double current, double speed) {
        return current + (target - current) * speed;
    }

    private void saveState() {
        try {
            Properties props = new Properties();
            props.setProperty("x", String.valueOf(posX));
            props.setProperty("y", String.valueOf(posY));
            props.setProperty("maximized", String.valueOf(isMaximized));
            if (selectedCategory != null) props.setProperty("category", selectedCategory.name());
            if (selectedModule != null) props.setProperty("module", selectedModule.getName());
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                props.store(writer, "Astra ClickGui State");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadState() {
        try {
            if (CONFIG_FILE.exists()) {
                Properties props = new Properties();
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    props.load(reader);
                    posX = Float.parseFloat(props.getProperty("x", "150"));
                    posY = Float.parseFloat(props.getProperty("y", "80"));
                    isMaximized = Boolean.parseBoolean(props.getProperty("maximized", "false"));
                    if (isMaximized) toggleMaximize();
                    
                    String catName = props.getProperty("category", null);
                    if (catName != null) {
                        for (Category cat : Category.values()) {
                            if (cat.name().equalsIgnoreCase(catName)) {
                                selectedCategory = cat;
                                break;
                            }
                        }
                    }
                    String modName = props.getProperty("module", null);
                    if (modName != null) {
                        for (Module mod : Main.moduleManager.getModules()) {
                            if (mod.getName().equals(modName)) {
                                selectedModule = mod;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}