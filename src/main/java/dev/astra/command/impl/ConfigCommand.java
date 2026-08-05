package dev.astra.command.impl;

import dev.astra.command.Command;
import dev.astra.config.ConfigManager;
import dev.astra.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;

public class ConfigCommand implements Command {

    @Override
    public String[] getAliases() {
        return new String[]{"config"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendMessage("Usage: .config <save/load/list> <name>");
            return;
        }

        String action = args[0].toLowerCase();
        if (action.equals("save") && args.length == 2) {
            if (ConfigManager.saveConfig(args[1])) {
                ChatUtil.sendMessage("Successfully saved config: " + args[1]);
            } else {
                ChatUtil.sendMessage("Failed to save config: " + args[1]);
            }
        } else if (action.equals("load") && args.length == 2) {
            if (ConfigManager.loadConfig(args[1])) {
                ChatUtil.sendMessage("Successfully loaded config: " + args[1]);
            } else {
                ChatUtil.sendMessage("Config not found: " + args[1]);
            }
        } else if (action.equals("list")) {
            List<String> configs = ConfigManager.getConfigs();
            if (configs.isEmpty()) {
                ChatUtil.sendMessage("No configs found.");
            } else {
                ChatUtil.sendMessage("Available configs: " + String.join(", ", configs));
            }
        } else {
            ChatUtil.sendMessage("Usage: .config <save/load/list> <name>");
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("save");
            list.add("load");
            list.add("list");
        } else if (args.length == 2) {
            list.addAll(ConfigManager.getConfigs());
        }
        return list;
    }
}