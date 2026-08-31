package dev.astra.util.inventory;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.HashMap;
import java.util.Map;

public final class StackExtensions {
    private StackExtensions() {
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null;
    }

    public static int durability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getItemDamage();
    }

    public static int totalDurability(ItemStack stack) {
        double multiplier;
        if (stack.getItem() instanceof ItemArmor) {
            multiplier = 1 / (0.6 + (0.4 / (getEnchantmentLevel(stack, Enchantment.unbreaking) + 1)));
        } else {
            multiplier = getEnchantmentLevel(stack, Enchantment.unbreaking) + 1.0;
        }
        return (int) Math.round(multiplier * durability(stack));
    }

    public static Map<Enchantment, Integer> enchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        NBTTagList list = stack.getEnchantmentTagList();
        if (list == null || list.hasNoTags()) {
            return enchantments;
        }
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tagCompound = list.getCompoundTagAt(i);
            if (tagCompound.hasKey("ench") || tagCompound.hasKey("id")) {
                Enchantment enchantment = Enchantment.getEnchantmentById(tagCompound.getInteger("id"));
                if (enchantment != null) {
                    enchantments.put(enchantment, tagCompound.getInteger("lvl"));
                }
            }
        }
        return enchantments;
    }

    public static int enchantmentCount(ItemStack stack) {
        return enchantments(stack).size();
    }

    public static int enchantmentSum(ItemStack stack) {
        int sum = 0;
        for (Integer lvl : enchantments(stack).values()) {
            sum += lvl;
        }
        return sum;
    }

    public static int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        Integer lvl = enchantments(stack).get(enchantment);
        return lvl == null ? 0 : lvl;
    }

    public static boolean hasItemAgePassed(ItemStack stack, int delay) {
        if (isEmpty(stack)) {
            return true;
        }
        return System.currentTimeMillis() - stack.itemDelay >= delay;
    }

    public static float attackDamage(ItemStack stack) {
        double amount = 1.0;
        for (Object o : stack.getAttributeModifiers().get("generic.attackDamage")) {
            net.minecraft.entity.ai.attributes.AttributeModifier modifier = (net.minecraft.entity.ai.attributes.AttributeModifier) o;
            amount = modifier.getAmount();
            break;
        }
        return (float) (amount + 1.25 * getEnchantmentLevel(stack, Enchantment.sharpness));
    }

    public static boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata());
    }

    @SuppressWarnings("deprecation")
    public static boolean isFullBlock(Block block) {
        return block.isFullBlock();
    }

    public static boolean isRepairable(ItemStack stack) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        return item instanceof ItemTool
                || item instanceof ItemArmor
                || item instanceof ItemShears
                || item instanceof ItemSword;
    }

    @SuppressWarnings("deprecation")
    public static boolean isSuitableBlock(ItemStack stack) {
        Item item = stack == null ? null : stack.getItem();
        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock) item).getBlock();
            return isFullBlock(block) && !block.hasTileEntity()
                    && !(block instanceof BlockWorkbench) && !(block instanceof BlockContainer) && !(block instanceof BlockFalling);
        }
        return false;
    }
}
