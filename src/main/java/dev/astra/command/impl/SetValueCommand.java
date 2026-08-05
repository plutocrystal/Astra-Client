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
            ChatUtil.sendMessage("Usage: .set <module> <value> <value>");
            return;
        }

        String moduleName = args[0];
        String valueName = args[1];
        String valueStr = args[2];

        Module targetModule = Main.moduleManager.getModuleByName(moduleName);
        if (targetModule == null) {
            ChatUtil.sendMessage("Module not found: " + moduleName);
            return;
        }

        for (Value<?> value : targetModule.getValues()) {
            if (value.getName().equalsIgnoreCase(valueName)) {
                try {
                    if (value instanceof NumberValue) {
                        ((NumberValue) value).setValue(Double.parseDouble(valueStr));
                    } else if (value instanceof BooleanValue) {
                        boolean boolVal = valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("on") || valueStr.equals("1");
                        ((BooleanValue) value).setValue(boolVal);
                    } else if (value instanceof ModeValue) {
                        ((ModeValue) value).setMode(valueStr);
                    }
                    ChatUtil.sendMessage(targetModule.getName() + " - " + value.getName() + " set to " + valueStr);
                    return;
                } catch (Exception e) {
                    ChatUtil.sendMessage("Invalid value type for " + value.getName());
                    return;
                }
            }
        }
        ChatUtil.sendMessage("Value not found: " + valueName);
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
                    if (value.getName().equalsIgnoreCase(args[1])) {
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