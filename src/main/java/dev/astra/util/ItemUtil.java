package dev.astra.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ItemUtil {

    public enum ItemCategory {
        SWORD, PICKAXE, AXE, SHOVEL, HOE,
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, BOW,
        OTHER
    }

    public enum ItemType {
        SWORD, AXE, PICKAXE, GAPPLE, FOOD, WATER_BUCKET, LAVA_BUCKET,
        PEARL, THROWABLE, ROD, BLOCK, POTION, ARMOR, TRASH, OTHER
    }

    public static ItemCategory getCategory(ItemStack stack) {
        if (stack == null) return ItemCategory.OTHER;
        Item item = stack.getItem();
        if (item instanceof ItemSword) return ItemCategory.SWORD;
        if (item instanceof ItemPickaxe) return ItemCategory.PICKAXE;
        if (item instanceof ItemAxe) return ItemCategory.AXE;
        if (item instanceof ItemSpade) return ItemCategory.SHOVEL;
        if (item instanceof ItemHoe) return ItemCategory.HOE;
        if (item instanceof ItemBow) return ItemCategory.BOW;
        if (item instanceof ItemArmor) {
            switch (((ItemArmor) item).armorType) {
                case 0: return ItemCategory.HELMET;
                case 1: return ItemCategory.CHESTPLATE;
                case 2: return ItemCategory.LEGGINGS;
                case 3: return ItemCategory.BOOTS;
            }
        }
        return ItemCategory.OTHER;
    }

    public static double getScore(ItemStack stack) {
        if (stack == null) return -1;
        double score = 0;
        Item item = stack.getItem();

        if (item instanceof ItemSword) {
            score += ((ItemSword) item).getDamageVsEntity();
        } else if (item instanceof ItemTool) {
            score += stack.getStrVsBlock(Blocks.stone) * 10.0; 
        } else if (item instanceof ItemArmor) {
            score += ((ItemArmor) item).damageReduceAmount;
            score += ((ItemArmor) item).getArmorMaterial().getDurability(((ItemArmor) item).armorType) / 100.0;
        } else if (item instanceof ItemBow) {
            score += 10;
        }

        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(stack);
        if (map != null && !map.isEmpty()) {
            for (Integer id : map.keySet()) {
                int level = map.get(id);
                Enchantment ench = Enchantment.getEnchantmentById(id);
                if (ench != null) {
                    if (ench.type == EnumEnchantmentType.WEAPON || ench.type == EnumEnchantmentType.BOW) {
                        score += level * 2.5;
                    } else if (ench.type == EnumEnchantmentType.ARMOR) {
                        score += level * 1.5;
                    } else if (ench.type == EnumEnchantmentType.DIGGER) {
                        score += level * 2.0;
                    } else {
                        score += level * 0.5;
                    }
                }
            }
        }
        return score;
    }

    public static ItemType getItemType(ItemStack stack) {
        if (stack == null) return ItemType.OTHER;
        Item item = stack.getItem();

        if (item instanceof ItemSword) return ItemType.SWORD;
        if (item instanceof ItemPickaxe) return ItemType.PICKAXE;
        if (item instanceof ItemAxe) return ItemType.AXE;
        if (item instanceof ItemArmor) return ItemType.ARMOR;
        if (item == Items.golden_apple) return ItemType.GAPPLE;
        if (item == Items.ender_pearl) return ItemType.PEARL;
        if (item == Items.fishing_rod) return ItemType.ROD;
        if (item == Items.water_bucket) return ItemType.WATER_BUCKET;
        if (item == Items.lava_bucket) return ItemType.LAVA_BUCKET;
        if (item == Items.potionitem) return ItemType.POTION;
        if (item instanceof ItemFood) return ItemType.FOOD;
        if (item instanceof ItemBucket) return ItemType.LAVA_BUCKET; 
        if (item == Items.snowball || item == Items.egg) return ItemType.THROWABLE;
        if (item instanceof ItemBlock) return ItemType.BLOCK;

        if (item == Items.stick || item == Items.string || item == Items.wheat_seeds || 
            item == Items.bowl || item == Items.flint || item == Items.feather || 
            item == Items.leather || item == Items.bone) {
            return ItemType.TRASH;
        }

        return ItemType.OTHER;
    }

    public static double getArmorScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return -1;
        ItemArmor armor = (ItemArmor) stack.getItem();
        double score = armor.damageReduceAmount;
        score += armor.getArmorMaterial().getDurability(armor.armorType) / 100.0;

        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(stack);
        if (map != null && !map.isEmpty()) {
            for (Integer id : map.keySet()) {
                int level = map.get(id);
                Enchantment ench = Enchantment.getEnchantmentById(id);
                if (ench != null && ench.type == EnumEnchantmentType.ARMOR) {
                    score += level * 1.5;
                }
            }
        }
        return score;
    }
}