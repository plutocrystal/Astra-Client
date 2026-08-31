# RenderUtil 基础渲染方法 + 平滑圆角 + ClickGUI 圆角边缘

## 状态
- 用户已确认方案（默认圆角半径 8px）
- 代码文件编辑被权限规则暂时拦截，本文件为完整可执行实施方案

## 改动 1：重写 `src/main/java/dev/astra/util/RenderUtil.java`

完整新内容如下（保留原有 int 版方法，新增 GL 平滑渲染）：

```java
package dev.astra.util;

import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

public class RenderUtil {

    private static final int ARC_SEGMENTS = 10;

    public static void enableGL2D() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void disableGL2D() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopAttrib();
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        enableGL2D();
        setColor(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glEnd();
        disableGL2D();
    }

    public static void drawSquare(int x, int y, int size, int color) {
        Gui.drawRect(x, y, x + size, y + size, color);
    }

    public static void drawCircle(float centerX, float centerY, float radius, int color) {
        enableGL2D();
        setColor(color);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(centerX, centerY);
        for (int i = 0; i <= 360; i += 6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2f(centerX + (float) Math.cos(rad) * radius, centerY + (float) Math.sin(rad) * radius);
        }
        GL11.glEnd();
        disableGL2D();
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

    // 原有像素块风格圆角（保留兼容，全项目暂无调用方）
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

    // 平滑圆角：单个 TRIANGLE_FAN，四角统一半径
    public static void drawRoundedRect(float left, float top, float right, float bottom, float radius, int color) {
        drawRoundedRect(left, top, right, bottom, radius, color, true, true, true, true);
    }

    // 平滑圆角：按角控制（供标题栏等只圆上两角的场景）
    public static void drawRoundedRect(float left, float top, float right, float bottom, float radius, int color,
                                       boolean roundTopLeft, boolean roundTopRight, boolean roundBottomRight, boolean roundBottomLeft) {
        float x1 = Math.min(left, right);
        float x2 = Math.max(left, right);
        float y1 = Math.min(top, bottom);
        float y2 = Math.max(top, bottom);

        float maxRadius = Math.min(x2 - x1, y2 - y1) / 2.0f;
        float rTL = roundTopLeft ? Math.min(radius, maxRadius) : 0.0f;
        float rTR = roundTopRight ? Math.min(radius, maxRadius) : 0.0f;
        float rBR = roundBottomRight ? Math.min(radius, maxRadius) : 0.0f;
        float rBL = roundBottomLeft ? Math.min(radius, maxRadius) : 0.0f;

        enableGL2D();
        setColor(color);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f((x1 + x2) / 2.0f, (y1 + y2) / 2.0f);
        arcPath(x1 + rTL, y1 + rTL, rTL, 180.0f, 270.0f);
        arcPath(x2 - rTR, y1 + rTR, rTR, 270.0f, 360.0f);
        arcPath(x2 - rBR, y2 - rBR, rBR, 0.0f, 90.0f);
        arcPath(x1 + rBL, y2 - rBL, rBL, 90.0f, 180.0f);
        GL11.glEnd();
        disableGL2D();
    }

    // 圆角描边
    public static void drawRoundedRectOutline(float left, float top, float right, float bottom, float radius, int color) {
        float x1 = Math.min(left, right);
        float x2 = Math.max(left, right);
        float y1 = Math.min(top, bottom);
        float y2 = Math.max(top, bottom);

        float maxRadius = Math.min(x2 - x1, y2 - y1) / 2.0f;
        float r = Math.min(radius, maxRadius);
        if (r <= 0.0f) {
            drawOutline((int) x1, (int) y1, (int) x2, (int) y2, color);
            return;
        }

        enableGL2D();
        setColor(color);
        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        arcPath(x1 + r, y1 + r, r, 180.0f, 270.0f);
        arcPath(x2 - r, y1 + r, r, 270.0f, 360.0f);
        arcPath(x2 - r, y2 - r, r, 0.0f, 90.0f);
        arcPath(x1 + r, y2 - r, r, 90.0f, 180.0f);
        GL11.glEnd();
        disableGL2D();
    }

    private static void arcPath(float centerX, float centerY, float radius, float startAngle, float endAngle) {
        for (int i = 0; i <= ARC_SEGMENTS; i++) {
            float angle = (float) Math.toRadians(startAngle + (endAngle - startAngle) * i / ARC_SEGMENTS);
            GL11.glVertex2f(centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius);
        }
    }

    private static void setColor(int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0f;
        float red = (float) (color >> 16 & 255) / 255.0f;
        float green = (float) (color >> 8 & 255) / 255.0f;
        float blue = (float) (color & 255) / 255.0f;
        GL11.glColor4f(red, green, blue, alpha);
    }
}
```

## 改动 2：`src/main/java/dev/astra/gui/ClickGuiScreen.java`

### 2a. 新增 import（插在 `import dev.astra.module.Module;` 之后）

```java
import dev.astra.util.RenderUtil;
```

### 2b. 新增常量（放在 `private static final float WIN11_BUTTON_WIDTH = 35.0f;` 之后）

```java
    private static final float CORNER_RADIUS = 8.0f;
```

### 2c. `renderMainBackground()` 替换为：

```java
    private void renderMainBackground() {
        float radius = isMaximized ? 0.0f : CORNER_RADIUS;
        RenderUtil.drawRoundedRect(posX, posY, posX + currentWidth, posY + currentHeight, radius, COLOR_BACKGROUND.getRGB());
    }
```

### 2d. `renderHeader()` 中第一行矩形替换为（仅上两角圆角）：

原：
```java
        drawRect(posX, posY, currentWidth, HEADER_HEIGHT, COLOR_HEADER.getRGB());
```
新：
```java
        RenderUtil.drawRoundedRect(posX, posY, posX + currentWidth, posY + HEADER_HEIGHT,
                isMaximized ? 0.0f : CORNER_RADIUS, COLOR_HEADER.getRGB(), true, true, false, false);
```

### 2e. `renderWin11Buttons()` 中关闭按钮 hover 红块替换为（仅右上角圆角）：

原：
```java
        if (closeHover) drawRect(closeX, posY, WIN11_BUTTON_WIDTH, HEADER_HEIGHT, COLOR_WIN11_CLOSE_HOVER.getRGB());
```
新：
```java
        if (closeHover)
            RenderUtil.drawRoundedRect(closeX, posY, closeX + WIN11_BUTTON_WIDTH, posY + HEADER_HEIGHT,
                    isMaximized ? 0.0f : CORNER_RADIUS, COLOR_WIN11_CLOSE_HOVER.getRGB(), false, true, false, false);
```

最小化/最大化按钮位于窗口中部，保持原有直角绘制不变。

## 验证

1. `./gradlew compileJava` 编译通过
2. 进游戏打开 ClickGUI：窗口四角、标题栏顶部、关闭按钮 hover 均为平滑圆角；拖动、最大化（直角全屏）、颜色选择器等功能正常
