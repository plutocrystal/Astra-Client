package dev.astra.command;

import dev.astra.Main;
import dev.astra.command.impl.SetValueCommand;
import dev.astra.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private final SetValueCommand setValueCommand = new SetValueCommand();
    
    private boolean isTabCompleting = false;
    private String lastBasePrefix = "";
    private String lastReturnedWord = "";
    private List<String> tabList = new ArrayList<>();
    private int tabIndex = 0;

    public void registerCommand(Command command) {
        commands.add(command);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public boolean execute(String message) {
        if (!message.startsWith(".")) {
            return false;
        }

        message = message.substring(1);
        String[] split = message.split(" ");
        String commandName = split[0];
        String[] args = Arrays.copyOfRange(split, 1, split.length);

        for (Command command : commands) {
            for (String alias : command.getAliases()) {
                if (alias.equalsIgnoreCase(commandName)) {
                    command.execute(args);
                    return true;
                }
            }
        }

        if (split.length >= 3) {
            setValueCommand.execute(split);
            return true;
        }

        ChatUtil.sendMessage("Unknown command.");
        return true;
    }

    public String getTabComplete(String input) {
        if (!input.startsWith(".")) {
            return null;
        }

        int lastSpace = input.lastIndexOf(" ");
        String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);
        String current = lastSpace == -1 ? input.substring(1) : input.substring(lastSpace + 1);
        boolean isCommandName = (lastSpace == -1);

        if (isTabCompleting && prefix.equals(lastBasePrefix) && input.equals(lastReturnedWord)) {
            if (tabIndex >= tabList.size()) tabIndex = 0;
            String completedWord = tabList.get(tabIndex);
            tabIndex++;
            lastReturnedWord = prefix + (isCommandName ? "." + completedWord : completedWord);
            return lastReturnedWord;
        } else {
            isTabCompleting = true;
            lastBasePrefix = prefix;
            tabList.clear();
            tabIndex = 0;

            if (isCommandName) {
                for (Command cmd : commands) {
                    for (String alias : cmd.getAliases()) {
                        if (alias.toLowerCase().startsWith(current.toLowerCase())) {
                            tabList.add(alias);
                        }
                    }
                }
                for (dev.astra.module.Module mod : Main.moduleManager.getModules()) {
                    if (mod.getName().toLowerCase().startsWith(current.toLowerCase())) {
                        tabList.add(mod.getName());
                    }
                }
            } else {
                String[] split = input.substring(1).split(" ", -1);
                String cmdName = split[0];
                Command targetCmd = null;
                
                for (Command cmd : commands) {
                    for (String alias : cmd.getAliases()) {
                        if (alias.equalsIgnoreCase(cmdName)) {
                            targetCmd = cmd;
                            break;
                        }
                    }
                }

                if (targetCmd != null) {
                    String[] args = Arrays.copyOfRange(split, 1, split.length);
                    List<String> results = targetCmd.tabComplete(args);
                    for (String s : results) {
                        if (s.toLowerCase().startsWith(current.toLowerCase())) {
                            tabList.add(s);
                        }
                    }
                } else {
                    String[] args = split;
                    List<String> results = setValueCommand.tabComplete(args);
                    for (String s : results) {
                        if (s.toLowerCase().startsWith(current.toLowerCase())) {
                            tabList.add(s);
                        }
                    }
                }
            }

            if (tabList.isEmpty()) {
                lastReturnedWord = input;
                return null;
            }

            if (tabIndex >= tabList.size()) tabIndex = 0;
            String completedWord = tabList.get(tabIndex);
            tabIndex++;
            lastReturnedWord = prefix + (isCommandName ? "." + completedWord : completedWord);
            return lastReturnedWord;
        }
    }
}