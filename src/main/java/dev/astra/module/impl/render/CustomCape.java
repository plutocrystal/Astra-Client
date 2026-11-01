package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CustomCape extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final File CAPE_FOLDER = new File(mc.mcDataDir, "Astra/CustomCape");

    public ModeValue capeMode;

    public BooleanValue reload;

    private final Set<ResourceLocation> loadedTextures = new HashSet<>();

    public CustomCape() {
        super("CustomCape", Keyboard.KEY_NONE, Category.RENDER);
        List<String> capes = getCapes();
        capes.add(0, "None");
        capeMode = new ModeValue("Cape", "None", capes.toArray(new String[0]));
        reload = new BooleanValue("Reload", false);
        addValues(capeMode, reload);
    }

    @Override
    public String getStatus() {
        String current = capeMode.getValue();
        return current.equals("None") ? "" : current;
    }

    public ResourceLocation getCurrentCape() {
        String current = capeMode.getValue();
        if (current.equals("None")) return null;
        File ext = new File(CAPE_FOLDER, current + ".png");
        if (ext.exists()) {
            return loadExternal(ext, current);
        }
        return new ResourceLocation("minecraft", "astra/texture/cape/" + current + ".png");
    }

    private ResourceLocation loadExternal(File file, String name) {
        ResourceLocation loc = new ResourceLocation("astra", "customcape/" + name);
        try {
            if (mc.getTextureManager().getTexture(loc) == null) {
                BufferedImage image = ImageIO.read(file);
                if (image == null) return null;
                mc.getTextureManager().loadTexture(loc, new DynamicTexture(image));
                loadedTextures.add(loc);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return loc;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (reload.isEnabled()) {
            reload.setValue(false);
            doReload();
        }
    }

    public void doReload() {
        for (ResourceLocation loc : loadedTextures) {
            mc.getTextureManager().deleteTexture(loc);
        }
        loadedTextures.clear();
        List<String> capes = getCapes();
        capes.add(0, "None");
        capeMode.setModes(capes);
    }

    private List<String> getCapes() {
        Set<String> names = new HashSet<>();
        if (!CAPE_FOLDER.exists()) {
            CAPE_FOLDER.mkdirs();
        }
        File[] files = CAPE_FOLDER.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".png")) {
                    names.add(f.getName().substring(0, f.getName().length() - 4));
                }
            }
        }
        String path = "assets/minecraft/astra/texture/cape";
        try {
            URL url = CustomCape.class.getClassLoader().getResource(path);
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    File dir = new File(url.toURI());
                    if (dir.exists() && dir.isDirectory()) {
                        for (File f : dir.listFiles()) {
                            if (f.getName().endsWith(".png")) {
                                names.add(f.getName().replace(".png", ""));
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
                                names.add(name.substring(name.lastIndexOf('/') + 1).replace(".png", ""));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(names);
    }
}
