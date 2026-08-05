package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class ToggleCommand implements Command {

    @Override
    public String[] getAliases() {
        return new String[]{"toggle"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendMessage("Usage: .toggle <module>");
            return;
        }

        String moduleName = args[0];
        for (Module module : Main.moduleManager.getModules()) {
            if (module.getName().equalsIgnoreCase(moduleName)) {
                module.toggle();
                ChatUtil.sendMessage(module.getName() + " is now " + (module.isToggle() ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                return;
            }
        }

        ChatUtil.sendMessage("Module not found: " + moduleName);
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String current = args[0];
            for (Module module : Main.moduleManager.getModules()) {
                if (module.getName().toLowerCase().startsWith(current.toLowerCase())) {
                    list.add(module.getName());
                }
            }
        }
        return list;
    }
}