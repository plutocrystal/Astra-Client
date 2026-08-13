package dev.astra.command.impl;

import dev.astra.Main;
import dev.astra.command.Command;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import dev.astra.value.Value;
import dev.astra.value.impl.BooleanValue;
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
            valueNameParts.add(args[i]);
        }

        String combinedNoSpaces = String.join("", valueNameParts).replace("\"", "");
        String combinedWithSpaces = String.join(" ", valueNameParts).replace("\"", "");

        Value<?> targetValue = null;
        for (Value<?> value : targetModule.getValues()) {
            String valName = value.getName().replace("\"", "");
            if (valName.equalsIgnoreCase(combinedWithSpaces)) {
                targetValue = value;
                break;
            }
            if (valName.equalsIgnoreCase(combinedNoSpaces)) {
                targetValue = value;
                break;
            }
            if (valName.replace(" ", "").equalsIgnoreCase(combinedNoSpaces)) {
                targetValue = value;
                break;
            }
        }

        if (targetValue == null) {
            ChatUtil.sendMessage("Value not found: " + combinedWithSpaces);
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
                }
            }
        } else if (args.length == 3) {
            Module targetModule = Main.moduleManager.getModuleByName(args[0]);
            if (targetModule != null) {
                for (Value<?> value : targetModule.getValues()) {
                    if (value.getName().replace(" ", "").equalsIgnoreCase(args[1].replace("\"", ""))) {
                        if (value instanceof BooleanValue) {
                            list.add("true");
                            list.add("false");
                        } else if (value instanceof ModeValue) {
                            list.addAll(((ModeValue) value).getModes());
                        }
                        break;
                    }
                }
            }
        }
        return list;
    }
}