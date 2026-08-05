package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements Command {

    @Override
    public String[] getAliases() {
        return new String[]{"help"};
    }

    @Override
    public void execute(String[] args) {
        ChatUtil.sendMessage("Available commands:");
        for (Command cmd : Main.commandManager.getCommands()) {
            ChatUtil.sendMessage(" - " + String.join(", ", cmd.getAliases()));
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        return new ArrayList<>();
    }
}