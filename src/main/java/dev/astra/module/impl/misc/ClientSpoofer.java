package dev.astra.module.impl.misc;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ModeValue;

public class ClientSpoofer extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "LunarClient", "Badlion", "Forge", "Labymod", "FML", "CmPack");

    public ClientSpoofer() {
        super("ClientSpoofer", 0, Category.MISC);
        addValues(mode);
    }

    @Override
    public String getStatus() {
        return mode.getValue();
    }

    public String getBrand() {
        switch (mode.getValue()) {
            case "Vanilla": return "vanilla";
            case "LunarClient": return "lunarclient:v2.12.0";
            case "Badlion": return "badlion";
            case "Forge": return "forge";
            case "Labymod": return "labymod";
            case "FML": return "fml";
            case "CmPack": return "cmpack";
            default: return null;
        }
    }
}
