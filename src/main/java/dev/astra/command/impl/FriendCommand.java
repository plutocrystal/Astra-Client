package dev.astra.command.impl;

import dev.astra.command.Command;
import dev.astra.util.ChatUtil;
import dev.astra.util.FriendManager;
import java.util.ArrayList;
import java.util.List;

public class FriendCommand implements Command {
    @Override
    public String[] getAliases() {
        return new String[]{"friend"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendMessage("Usage: .friend <add|remove|list|clear> [name]");
            return;
        }
        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                if (args.length < 2) {
                    ChatUtil.sendMessage("Usage: .friend add <name>");
                    return;
                }
                FriendManager.addFriend(args[1]);
                ChatUtil.sendMessage("Added friend: " + args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    ChatUtil.sendMessage("Usage: .friend remove <name>");
                    return;
                }
                FriendManager.removeFriend(args[1]);
                ChatUtil.sendMessage("Removed friend: " + args[1]);
                break;
            case "list":
                List<String> friends = FriendManager.getFriends();
                if (friends.isEmpty()) {
                    ChatUtil.sendMessage("No friends.");
                } else {
                    ChatUtil.sendMessage("Friends: " + String.join(", ", friends));
                }
                break;
            case "clear":
                FriendManager.clearFriends();
                ChatUtil.sendMessage("Cleared all friends.");
                break;
            default:
                ChatUtil.sendMessage("Usage: .friend <add|remove|list|clear> [name]");
                break;
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            String[] actions = {"add", "remove", "list", "clear"};
            for (String a : actions) {
                if (a.startsWith(current)) {
                    list.add(a);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            String current = args[1].toLowerCase();
            for (String f : FriendManager.getFriends()) {
                if (f.startsWith(current)) {
                    list.add(f);
                }
            }
        }
        return list;
    }
}
