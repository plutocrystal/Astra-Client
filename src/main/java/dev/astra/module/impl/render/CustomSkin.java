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

public class CustomSkin extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final File SKIN_FOLDER = new File(mc.mcDataDir, "Astra/CustomSkin");

    public ModeValue skinMode;

    public ModeValue model;

    public BooleanValue allPlayers;

    public BooleanValue reload;

    private final Set<ResourceLocation> loadedTextures = new HashSet<>();

    public CustomSkin() {
        super("CustomSkin", Keyboard.KEY_NONE, Category.RENDER);
        List<String> skins = getSkins();
        skins.add(0, "None");
        skinMode = new ModeValue("Skin", "None", skins.toArray(new String[0]));
        model = new ModeValue("Model", "Steve", "Steve", "Alex");
        allPlayers = new BooleanValue("AllPlayers", false);
        reload = new BooleanValue("Reload", false);
        addValues(skinMode, model, allPlayers, reload);
    }

    @Override
    public String getStatus() {
        String current = skinMode.getValue();
        return current.equals("None") ? "" : current;
    }

    public ResourceLocation getCurrentSkin() {
        String current = skinMode.getValue();
        if (current.equals("None")) return null;
        File ext = new File(SKIN_FOLDER, current + ".png");
        if (ext.exists()) {
            return loadExternal(ext, current);
        }
        return new ResourceLocation("minecraft", "astra/texture/skin/" + current + ".png");
    }

    private ResourceLocation loadExternal(File file, String name) {
        ResourceLocation loc = new ResourceLocation("astra", "customskin/" + name);
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
        List<String> skins = getSkins();
        skins.add(0, "None");
        skinMode.setModes(skins);
    }

    public boolean shouldApply(net.minecraft.client.entity.AbstractClientPlayer player) {
        if (skinMode.getValue().equals("None")) return false;
        if (allPlayers.isEnabled()) return true;
        return player == net.minecraft.client.Minecraft.getMinecraft().thePlayer;
    }

    public String getSkinType() {
        return model.getValue().equals("Alex") ? "slim" : "default";
    }

    private List<String> getSkins() {
        Set<String> names = new HashSet<>();
        if (!SKIN_FOLDER.exists()) {
            SKIN_FOLDER.mkdirs();
        }
        File[] files = SKIN_FOLDER.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".png")) {
                    names.add(f.getName().substring(0, f.getName().length() - 4));
                }
            }
        }
        String path = "assets/minecraft/astra/texture/skin";
        try {
            URL url = CustomSkin.class.getClassLoader().getResource(path);
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
