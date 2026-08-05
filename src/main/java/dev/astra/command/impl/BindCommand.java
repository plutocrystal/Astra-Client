package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class BindCommand implements Command {

    @Override
    public String[] getAliases() {
        return new String[]{"bind"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            ChatUtil.sendMessage("Usage: .bind <module> <key>");
            return;
        }

        String moduleName = args[0];
        String keyName = args[1].toUpperCase();
        int key = Keyboard.getKeyIndex(keyName);

        for (Module module : Main.moduleManager.getModules()) {
            if (module.getName().equalsIgnoreCase(moduleName)) {
                module.setKeycode(key);
                ChatUtil.sendMessage("Bound " + module.getName() + " to " + Keyboard.getKeyName(key));
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