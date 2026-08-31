package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.module.impl.render.CustomCape;
import dev.astra.module.impl.render.CustomSkin;
import dev.astra.util.ChatUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReloadCommand implements Command {
    @Override
    public String[] getAliases() {
        return new String[]{"reload"};
    }

    @Override
    public void execute(String[] args) {
        String target = args.length == 0 ? "all" : args[0].toLowerCase();
        if (target.equals("skin") || target.equals("cape") || target.equals("all")) {
            if (target.equals("skin") || target.equals("all")) {
                CustomSkin customSkin = Main.moduleManager.getModuleByClass(CustomSkin.class);
                if (customSkin != null) {
                    customSkin.doReload();
                }
            }
            if (target.equals("cape") || target.equals("all")) {
                CustomCape customCape = Main.moduleManager.getModuleByClass(CustomCape.class);
                if (customCape != null) {
                    customCape.doReload();
                }
            }
            ChatUtil.sendMessage("Successfully reloaded: " + target);
        } else {
            ChatUtil.sendMessage("Usage: .reload <skin/cape/all>");
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Arrays.asList("skin", "cape", "all"));
        }
        return new ArrayList<>();
    }
}
