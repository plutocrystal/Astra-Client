package dev.astra.command;

import java.util.List;

public interface Command {
    String[] getAliases();
    void execute(String[] args);
    List<String> tabComplete(String[] args);
}