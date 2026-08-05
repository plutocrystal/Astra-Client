package dev.astra.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ChatUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final String PREFIX = EnumChatFormatting.GOLD + "[" + EnumChatFormatting.BLUE + "Astra" + EnumChatFormatting.GOLD + "] " + EnumChatFormatting.RESET;

    public static void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + message));
        }
    }
}