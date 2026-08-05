package dev.astra.config;

import dev.astra.Main;
import dev.astra.module.Module;
import dev.astra.value.Value;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final File CONFIG_DIR = new File("Astra/config/");

    public static void init() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
    }

    public static boolean saveConfig(String name) {
        init();
        JsonObject root = new JsonObject();
        JsonArray modulesArray = new JsonArray();

        for (Module module : Main.moduleManager.getModules()) {
            JsonObject modJson = new JsonObject();
            modJson.addProperty("name", module.getName());
            modJson.addProperty("enabled", module.isToggle());
            modJson.addProperty("keycode", module.getKeycode());

            JsonObject valuesJson = new JsonObject();
            for (Value<?> value : module.getValues()) {
                if (value instanceof BooleanValue) {
                    valuesJson.addProperty(value.getName(), ((BooleanValue) value).getValue());
                } else if (value instanceof NumberValue) {
                    valuesJson.addProperty(value.getName(), ((NumberValue) value).getValue());
                } else if (value instanceof ModeValue) {
                    valuesJson.addProperty(value.getName(), ((ModeValue) value).getValue());
                }
            }
            modJson.add("values", valuesJson);
            modulesArray.add(modJson);
        }
        root.add("modules", modulesArray);

        File file = new File(CONFIG_DIR, name + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loadConfig(String name) {
        init();
        File file = new File(CONFIG_DIR, name + ".json");
        if (!file.exists()) return false;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            JsonArray modulesArray = root.getAsJsonArray("modules");

            for (int i = 0; i < modulesArray.size(); i++) {
                JsonObject modJson = modulesArray.get(i).getAsJsonObject();
                String modName = modJson.get("name").getAsString();
                
                Module module = Main.moduleManager.getModuleByName(modName);
                if (module != null) {
                    if (modJson.has("enabled")) {
                        boolean enabled = modJson.get("enabled").getAsBoolean();
                        if (module.isToggle() != enabled) {
                            module.toggle();
                        }
                    }
                    if (modJson.has("keycode")) {
                        module.setKeycode(modJson.get("keycode").getAsInt());
                    }
                    if (modJson.has("values")) {
                        JsonObject valuesJson = modJson.getAsJsonObject("values");
                        for (Value<?> value : module.getValues()) {
                            if (valuesJson.has(value.getName())) {
                                if (value instanceof BooleanValue) {
                                    ((BooleanValue) value).setValue(valuesJson.get(value.getName()).getAsBoolean());
                                } else if (value instanceof NumberValue) {
                                    ((NumberValue) value).setValue(valuesJson.get(value.getName()).getAsDouble());
                                } else if (value instanceof ModeValue) {
                                    ((ModeValue) value).setMode(valuesJson.get(value.getName()).getAsString());
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getConfigs() {
        init();
        List<String> configs = new ArrayList<>();
        File[] files = CONFIG_DIR.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".json")) {
                    configs.add(f.getName().replace(".json", ""));
                }
            }
        }
        return configs;
    }
}