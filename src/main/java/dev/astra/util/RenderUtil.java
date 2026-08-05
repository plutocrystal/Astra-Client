package dev.astra.util;

import net.minecraft.client.gui.Gui;

public class RenderUtil {

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawSquare(int x, int y, int size, int color) {
        Gui.drawRect(x, y, x + size, y + size, color);
    }

    public static void drawOutline(int left, int top, int right, int bottom, int color) {
        int x1 = Math.min(left, right);
        int x2 = Math.max(left, right);
        int y1 = Math.min(top, bottom);
        int y2 = Math.max(top, bottom);
        
        Gui.drawRect(x1, y1, x2, y1 + 1, color);
        Gui.drawRect(x1, y2 - 1, x2, y2, color);
        Gui.drawRect(x1, y1, x1 + 1, y2, color);
        Gui.drawRect(x2 - 1, y1, x2, y2, color);
    }

    public static void drawHorizontalLine(int startX, int endX, int y, int color) {
        Gui.drawRect(Math.min(startX, endX), y, Math.max(startX, endX) + 1, y + 1, color);
    }

    public static void drawVerticalLine(int x, int startY, int endY, int color) {
        Gui.drawRect(x, Math.min(startY, endY), x + 1, Math.max(startY, endY) + 1, color);
    }

    public static void drawRoundedRect(int left, int top, int right, int bottom, int radius, int color) {
        int x1 = Math.min(left, right);
        int x2 = Math.max(left, right);
        int y1 = Math.min(top, bottom);
        int y2 = Math.max(top, bottom);
        
        if (radius > (y2 - y1) / 2 || radius > (x2 - x1) / 2) {
            radius = Math.min((y2 - y1) / 2, (x2 - x1) / 2);
        }
        if (radius <= 0) {
            Gui.drawRect(x1, y1, x2, y2, color);
            return;
        }

        Gui.drawRect(x1 + radius, y1, x2 - radius, y2, color);
        Gui.drawRect(x1, y1 + radius, x2, y2 - radius, color);

        for (int i = 0; i < radius; i++) {
            int width = (int) Math.round(radius * Math.sin(Math.toRadians(90.0 * i / radius)));
            Gui.drawRect(x1 + radius - width, y1 + i, x2 - radius + width, y1 + i + 1, color);
            Gui.drawRect(x1 + radius - width, y2 - i - 1, x2 - radius + width, y2 - i, color);
        }
    }
}