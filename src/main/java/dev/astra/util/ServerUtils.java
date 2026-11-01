package dev.astra.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

public class ServerUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static int hypixelStatus() {
        ServerData data = mc.getCurrentServerData();
        if (data != null && data.serverIP != null && data.serverIP.toLowerCase().contains("hypixel")) {
            return 1;
        }
        return 0;
    }

    public static int getLobbyStatus() {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return 0;
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return 0;
        }
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective != null && objective.getDisplayName() != null
                && objective.getDisplayName().toLowerCase().contains("lobby")) {
            return 1;
        }
        return 0;
    }

    public static boolean spectatorCheck() {
        return mc.thePlayer != null && mc.thePlayer.isSpectator();
    }

    public static boolean isReplay() {
        return false;
    }
}
