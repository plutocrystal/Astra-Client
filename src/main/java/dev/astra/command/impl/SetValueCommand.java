package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import dev.astra.value.Value;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.GroupValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import java.util.ArrayList;
import java.util.List;

public class SetValueCommand implements Command {
    @Override
    public String[] getAliases() {
        return new String[]{"set"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3) {
            ChatUtil.sendMessage("Usage: .set <module> <value> <value> (Use quotes for spaces)");
            return;
        }
        String moduleName = args[0];
        Module targetModule = Main.moduleManager.getModuleByName(moduleName);
        if (targetModule == null) {
            ChatUtil.sendMessage("Module not found: " + moduleName);
            return;
        }
        String valueStr = args[args.length - 1];
        List<String> valueNameParts = new ArrayList<>();
        for (int i = 1; i < args.length - 1; i++) {
            for (String sub : args[i].replace("\"", "").split("\\.")) {
                if (!sub.isEmpty()) {
                    valueNameParts.add(sub);
                }
            }
        }
        String combinedNoSpaces = String.join("", valueNameParts).replace("\"", "");
        Value<?> targetValue = findValue(targetModule.getValues(), valueNameParts.toArray(new String[0]), 0);
        if (targetValue == null && !combinedNoSpaces.isEmpty()) {
            targetValue = findValue(targetModule.getValues(), new String[]{combinedNoSpaces.replace(" ", "")}, 0);
        }
        if (targetValue == null) {
            ChatUtil.sendMessage("Value not found: " + String.join(".", valueNameParts));
            return;
        }
        try {
            if (targetValue instanceof NumberValue) {
                ((NumberValue) targetValue).setValue(Double.parseDouble(valueStr));
            } else if (targetValue instanceof BooleanValue) {
                boolean boolVal = valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("on") || valueStr.equals("1");
                ((BooleanValue) targetValue).setValue(boolVal);
            } else if (targetValue instanceof ModeValue) {
                ((ModeValue) targetValue).setMode(valueStr);
            }
            ChatUtil.sendMessage(targetModule.getName() + " - " + targetValue.getName() + " set to " + valueStr);
        } catch (Exception e) {
            ChatUtil.sendMessage("Invalid value type for " + targetValue.getName());
        }
    }

    private Value<?> findValue(List<Value<?>> valuesList, String[] path, int index) {
        if (path.length == 0 || index >= path.length) return null;
        String part = path[index].replace(" ", "").replace("\"", "");
        for (Value<?> value : valuesList) {
            String valName = value.getName().replace(" ", "").replace("\"", "");
            if (valName.equalsIgnoreCase(part)) {
                if (index == path.length - 1) return value;
                if (value instanceof GroupValue) {
                    Value<?> found = findValue(((GroupValue) value).getChildren(), path, index + 1);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (Module module : Main.moduleManager.getModules()) {
                list.add(module.getName());
            }
        } else if (args.length == 2) {
            Module targetModule = Main.moduleManager.getModuleByName(args[0]);
            if (targetModule != null) {
                for (Value<?> value : targetModule.getValues()) {
                    list.add(value.getName());
                    if (value instanceof GroupValue) {
                        for (Value<?> child : ((GroupValue) value).getChildren()) {
                            list.add(value.getName() + "." + child.getName());
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            Module targetModule = Main.moduleManager.getModuleByName(args[0]);
            if (targetModule != null) {
                String[] path = args[1].replace("\"", "").split("\\.");
                Value<?> targetValue = findValue(targetModule.getValues(), path, 0);
                if (targetValue == null) {
                    String flattened = args[1].replace("\"", "").replace(" ", "");
                    targetValue = findValue(targetModule.getValues(), new String[]{flattened.replace(".", "")}, 0);
                }
                if (targetValue instanceof BooleanValue) {
                    list.add("true");
                    list.add("false");
                } else if (targetValue instanceof ModeValue) {
                    list.addAll(((ModeValue) targetValue).getModes());
                }
            }
        }
        return list;
    }
}
