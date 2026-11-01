package dev.astra.util.customfont;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Fonts {
    private static final String FONT_PATH = "/assets/minecraft/astra/font/%s.ttf";

    private static final Map<String, CustomFont> cache = new ConcurrentHashMap<>();

    public static CustomFont esp() {
        return get("esp", 14.0F);
    }

    public static CustomFont get(String name, float size) {
        String key = name.toLowerCase(Locale.ROOT) + "@" + size;
        CustomFont font = cache.get(key);
        if (font != null) {
            return font;
        }
        InputStream stream = Fonts.class.getResourceAsStream(String.format(FONT_PATH, name));
        if (stream == null) {
            return null;
        }
        font = CustomFont.create(stream, size);
        try {
            stream.close();
        } catch (Exception ignored) {
        }
        if (font != null) {
            cache.put(key, font);
        }
        return font;
    }
}
