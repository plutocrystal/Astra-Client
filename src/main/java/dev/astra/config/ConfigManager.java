package dev.astra.config;

import dev.astra.Main;
import dev.astra.module.Module;
import dev.astra.util.inventory.LBInventoryManager;
import dev.astra.value.Value;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.GroupValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.MultiBooleanValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import dev.astra.value.impl.TextValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {
   private static final File CONFIG_DIR = new File("Astra/Config/");

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
           modJson.addProperty("hidden", module.isHidden());
            JsonObject valuesJson = new JsonObject();
            for (Value<?> value : module.getValues()) {
                saveValue(valuesJson, value);
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
            java.util.Set<String> configNames = new java.util.HashSet<>();
            for (int i = 0; i < modulesArray.size(); i++) {
                JsonObject modJson = modulesArray.get(i).getAsJsonObject();
                if (modJson.has("name")) {
                    configNames.add(modJson.get("name").getAsString().toLowerCase());
                }
            }
            for (Module module : Main.moduleManager.getModules()) {
                if (module.isToggle() && !configNames.contains(module.getName().toLowerCase())) {
                    module.toggle();
                }
            }
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
                   if (modJson.has("hidden")) {
                       module.setHidden(modJson.get("hidden").getAsBoolean());
                   }
                    if (modJson.has("values")) {
                        JsonObject valuesJson = modJson.getAsJsonObject("values");
                        for (Value<?> value : module.getValues()) {
                            loadValue(valuesJson, value);
                        }
                    }
                }
            }
            for (Module module : Main.moduleManager.getModules()) {
                module.refreshVisibility();
            }
            LBInventoryManager.refreshVisibility();
            return true;
       } catch (Exception e) {
           e.printStackTrace();
           return false;
       }
   }

   private static void saveValue(JsonObject parent, Value<?> value) {
       if (value instanceof GroupValue) {
           JsonObject groupJson = new JsonObject();
           for (Value<?> child : ((GroupValue) value).getChildren()) {
               saveValue(groupJson, child);
           }
           parent.add(value.getName(), groupJson);
       } else if (value instanceof BooleanValue) {
           parent.addProperty(value.getName(), ((BooleanValue) value).getValue());
       } else if (value instanceof NumberValue) {
           parent.addProperty(value.getName(), ((NumberValue) value).getValue());
       } else if (value instanceof ModeValue) {
           parent.addProperty(value.getName(), ((ModeValue) value).getValue());
       } else if (value instanceof ColorValue) {
           parent.addProperty(value.getName(), ((ColorValue) value).getValue());
       } else if (value instanceof MultiBooleanValue) {
           JsonObject multiJson = new JsonObject();
           for (Map.Entry<String, Boolean> entry : ((MultiBooleanValue) value).getValue().entrySet()) {
               multiJson.addProperty(entry.getKey(), entry.getValue());
           }
           parent.add(value.getName(), multiJson);
        } else if (value instanceof RangeSelectionNumberValue) {
            JsonArray rangeArray = new JsonArray();
            rangeArray.add(new JsonPrimitive(((RangeSelectionNumberValue) value).getMinValue()));
            rangeArray.add(new JsonPrimitive(((RangeSelectionNumberValue) value).getMaxValue()));
            parent.add(value.getName(), rangeArray);
        } else if (value instanceof TextValue) {
            parent.addProperty(value.getName(), ((TextValue) value).getValue());
        }
    }

   private static void loadValue(JsonObject parent, Value<?> value) {
       if (!parent.has(value.getName())) return;
       if (value instanceof GroupValue) {
           JsonObject groupJson = parent.getAsJsonObject(value.getName());
           for (Value<?> child : ((GroupValue) value).getChildren()) {
               loadValue(groupJson, child);
           }
       } else if (value instanceof BooleanValue) {
           ((BooleanValue) value).setValue(parent.get(value.getName()).getAsBoolean());
       } else if (value instanceof NumberValue) {
           ((NumberValue) value).setValue(parent.get(value.getName()).getAsDouble());
       } else if (value instanceof ModeValue) {
           ((ModeValue) value).setMode(parent.get(value.getName()).getAsString());
       } else if (value instanceof ColorValue) {
           ((ColorValue) value).setValue(parent.get(value.getName()).getAsInt());
       } else if (value instanceof MultiBooleanValue) {
           JsonObject multiJson = parent.getAsJsonObject(value.getName());
           MultiBooleanValue mbv = (MultiBooleanValue) value;
           for (Map.Entry<String, JsonElement> entry : multiJson.entrySet()) {
               mbv.setValue(entry.getKey(), entry.getValue().getAsBoolean());
           }
        } else if (value instanceof RangeSelectionNumberValue) {
            JsonArray rangeArray = parent.getAsJsonArray(value.getName());
            double minVal = rangeArray.get(0).getAsDouble();
            double maxVal = rangeArray.get(1).getAsDouble();
            ((RangeSelectionNumberValue) value).setValues(minVal, maxVal);
        } else if (value instanceof TextValue) {
            ((TextValue) value).setValue(parent.get(value.getName()).getAsString());
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
