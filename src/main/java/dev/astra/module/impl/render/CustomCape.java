package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ModeValue;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CustomCape extends Module {

    public ModeValue capeMode;

    public CustomCape() {
        super("CustomCape", Keyboard.KEY_NONE, Category.RENDER);
        List<String> capes = getCapes();
        capes.add(0, "None");
        capeMode = new ModeValue("Cape", "None", capes.toArray(new String[0]));
        addValues(capeMode);
    }

    public ResourceLocation getCurrentCape() {
        String current = capeMode.getValue();
        if (current.equals("None")) return null;
        return new ResourceLocation("minecraft", "astra/texture/cape/" + current + ".png");
    }

    private List<String> getCapes() {
        List<String> capes = new ArrayList<>();
        String path = "assets/minecraft/astra/texture/cape";
        try {
            URL url = CustomCape.class.getClassLoader().getResource(path);
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    File dir = new File(url.toURI());
                    if (dir.exists() && dir.isDirectory()) {
                        for (File f : dir.listFiles()) {
                            if (f.getName().endsWith(".png")) {
                                capes.add(f.getName().replace(".png", ""));
                            }
                        }
                    }
                } else if (url.getProtocol().equals("jar")) {
                    String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                    try (JarFile jar = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            String name = entries.nextElement().getName();
                            if (name.startsWith(path) && name.endsWith(".png")) {
                                String capeName = name.substring(name.lastIndexOf('/') + 1).replace(".png", "");
                                capes.add(capeName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return capes;
    }
}