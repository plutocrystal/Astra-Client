package dev.astra.value.impl;

import dev.astra.value.Value;

public class ColorValue extends Value<Integer> {
    public ColorValue(String name, int color) {
        super(name, color);
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