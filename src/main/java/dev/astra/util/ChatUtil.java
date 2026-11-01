package dev.astra.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ChatUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final String PREFIX = EnumChatFormatting.GOLD + "[" + EnumChatFormatting.BLUE + "Astra" + EnumChatFormatting.GOLD + "] " + EnumChatFormatting.RESET;

    private static final String COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";

    public static void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + message));
        }
    }

    public static String translateColorCodes(String input) {
        if (input == null || input.indexOf('&') < 0) {
            return input;
        }
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && COLOR_CODES.indexOf(chars[i + 1]) > -1) {
                chars[i] = '\u00a7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
