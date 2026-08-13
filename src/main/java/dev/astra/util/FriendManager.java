package dev.astra.utils;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class FriendManager {
    private static final File friendDir = new File(Minecraft.getMinecraft().mcDataDir, "Astra/Friend");
    private static final File friendFile = new File(friendDir, "friends.txt");
    private static final Set<String> friends = new HashSet<>();

    static {
        loadFriends();
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        return friends.contains(name.toLowerCase());
    }

    public static void addFriend(String name) {
        if (name != null) {
            friends.add(name.toLowerCase());
            saveFriends();
        }
    }

    public static void removeFriend(String name) {
        if (name != null) {
            friends.remove(name.toLowerCase());
            saveFriends();
        }
    }

    public static boolean toggleFriend(String name) {
        if (isFriend(name)) {
            removeFriend(name);
            return false;
        } else {
            addFriend(name);
            return true;
        }
    }

    private static void loadFriends() {
        try {
            if (!friendFile.exists()) return;
            BufferedReader reader = new BufferedReader(new FileReader(friendFile));
            String line;
            while ((line = reader.readLine()) != null) {
                friends.add(line.trim().toLowerCase());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveFriends() {
        try {
            if (!friendDir.exists()) {
                friendDir.mkdirs();
            }
            PrintWriter writer = new PrintWriter(new FileWriter(friendFile));
            for (String friend : friends) {
                writer.println(friend);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}