package dev.astra.value.impl;

import dev.astra.value.Value;

public class ColorValue extends Value<Integer> {
    private final boolean alpha;
    private BooleanValue toggle;
    private boolean expanded = false;

    public ColorValue(String name, int color) {
        this(name, color, false);
    }

    public ColorValue(String name, int color, boolean alpha) {
        super(name, color);
        this.alpha = alpha;
    }

    public boolean hasAlpha() {
        return alpha;
    }

    public BooleanValue getToggle() {
        return toggle;
    }

    public void setToggle(BooleanValue toggle) {
        this.toggle = toggle;
    }

    public boolean isEnabled() {
        return toggle == null || toggle.getValue();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public void setEnabled(boolean enabled) {
        if (toggle != null) {
            toggle.setValue(enabled);
        }
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public int getAlpha() {
        return (getValue() >> 24) & 0xFF;
    }

    public void setColor(int r, int g, int b, int a) {
        setValue((a << 24) | (r << 16) | (g << 8) | b);
    }
}