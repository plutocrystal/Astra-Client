package dev.astra.util.customfont;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CustomFont {
    private static class Glyph {
        int textureId = -1;
        float width;
        float height;
        boolean empty;
    }

    private final java.awt.Font font;

    private final Map<Character, Glyph> glyphs = new HashMap<>();

    private final float fontHeight;

    private final float ascent;

    public static CustomFont create(InputStream ttfStream, float size) {
        try {
            return new CustomFont(ttfStream, size);
        } catch (Exception e) {
            return null;
        }
    }

    public CustomFont(InputStream ttfStream, float size) throws Exception {
        this.font = Font.createFont(Font.TRUETYPE_FONT, ttfStream).deriveFont(Font.PLAIN, size);
        Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        FontMetrics metrics = graphics.getFontMetrics(this.font);
        this.fontHeight = metrics.getHeight();
        this.ascent = metrics.getAscent();
        graphics.dispose();
    }

    public float getHeight() {
        return fontHeight;
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0F;
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            width += getGlyph(text.charAt(i)).width;
        }
        return width;
    }

    public float drawString(String text, float x, float y, int argb) {
        return drawInternal(text, x, y, argb, false);
    }

    public float drawStringWithShadow(String text, float x, float y, int argb) {
        return drawInternal(text, x, y, argb, true);
    }

    private float drawInternal(String text, float x, float y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return 0.0F;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        float offset = 0.0F;
        if (shadow) {
            renderText(text, 1.0F, 1.0F, (argb & 0xFF000000));
        }
        offset = renderText(text, 0.0F, 0.0F, argb);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
        return offset;
    }

    private float renderText(String text, float offsetX, float offsetY, int argb) {
        applyColor(argb);
        float cursor = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = getGlyph(text.charAt(i));
            if (!glyph.empty && glyph.textureId != -1) {
                GlStateManager.bindTexture(glyph.textureId);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0.0F, 0.0F);
                GL11.glVertex2f(cursor + offsetX, offsetY);
                GL11.glTexCoord2f(0.0F, 1.0F);
                GL11.glVertex2f(cursor + offsetX, offsetY + glyph.height);
                GL11.glTexCoord2f(1.0F, 1.0F);
                GL11.glVertex2f(cursor + offsetX + glyph.width, offsetY + glyph.height);
                GL11.glTexCoord2f(1.0F, 0.0F);
                GL11.glVertex2f(cursor + offsetX + glyph.width, offsetY);
                GL11.glEnd();
            }
            cursor += glyph.width;
        }
        return cursor;
    }

    private void applyColor(int argb) {
        GlStateManager.color(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, ((argb >> 24) & 0xFF) / 255.0F);
    }

    private Glyph getGlyph(char c) {
        Glyph glyph = glyphs.get(c);
        if (glyph != null) {
            return glyph;
        }
        glyph = new Glyph();
        glyphs.put(c, glyph);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(font);
        FontMetrics metrics = probeGraphics.getFontMetrics();
        int width = metrics.charWidth(c);
        probeGraphics.dispose();
        glyph.width = Math.max(width, 1.0F);
        glyph.height = fontHeight;
        if (Character.isWhitespace(c) || width <= 0) {
            glyph.empty = true;
            return glyph;
        }
        try {
            BufferedImage image = new BufferedImage(Math.max(width, 1), Math.max((int) fontHeight, 1), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.drawString(String.valueOf(c), 0, ascent);
            graphics.dispose();
            glyph.textureId = TextureUtil.uploadTextureImageAllocate(GlStateManager.generateTexture(), image, false, true);
        } catch (Exception e) {
            glyph.empty = true;
        }
        return glyph;
    }
}
