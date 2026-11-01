package dev.astra.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.util.ResourceLocation;

/**
 * Ported from LiquidBounce legacy (net.ccbluex.liquidbounce.utils.inventory.ItemUtils#createItem).
 */
public class ItemUtil {

    private ItemUtil() {
    }

    /**
     * Creates an ItemStack from a string in the format
     * {@code <item> [amount] [data] [datatag]}.
     *
     * @return the created stack or {@code null} if parsing failed
     */
    public static ItemStack createItem(String itemArguments) {
        try {
            String[] args = itemArguments.replace('&', '\u00a7').split(" ");

            int amount = 1;
            if (args.length > 1) {
                Integer parsed = tryParseInt(args[1]);
                if (parsed != null) amount = parsed;
            }

            int meta = 0;
            if (args.length > 2) {
                Integer parsed = tryParseInt(args[2]);
                if (parsed != null) meta = parsed;
            }

            ResourceLocation resourceLocation = new ResourceLocation(args[0]);
            Item item = (Item) Item.itemRegistry.getObject(resourceLocation);
            if (item == null) return null;

            ItemStack itemStack = new ItemStack(item, amount, meta);

            if (args.length >= 4) {
                StringBuilder nbt = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    nbt.append(args[i]).append(' ');
                }
                try {
                    itemStack.setTagCompound(JsonToNBT.getTagFromJson(nbt.toString().trim()));
                } catch (NBTException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return itemStack;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Integer tryParseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
