package dev.astra.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public class ItemUtil {
    public static boolean isUseful(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        if (item instanceof ItemArmor || item instanceof ItemSword || item instanceof ItemBow || item instanceof ItemTool || item instanceof ItemHoe) return true;
        if (item instanceof ItemBlock) return true;
        if (item instanceof ItemFood) return true;
        if (item == Items.ender_pearl || item == Items.arrow || item == Items.experience_bottle) return true;
        if (item == Items.bucket || item == Items.water_bucket || item == Items.lava_bucket || item == Items.milk_bucket) return true;
        if (item == Items.potionitem) return true;
        if (item == Items.fishing_rod) return true;
        if (item == Items.egg || item == Items.snowball) return true;
        return false;
    }

    public static boolean isBadPotion(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.potionitem) return false;
        List<PotionEffect> effects = Items.potionitem.getEffects(stack);
        if (effects == null) return false;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion != null && potion.isBadEffect()) {
                return true;
            }
        }
        return false;
    }

    public static String getCategory(ItemStack stack) {
        if (stack == null) return "None";
        Item item = stack.getItem();
        if (item instanceof ItemSword) return "Sword";
        if (item instanceof ItemBow) return "Bow";
        if (item instanceof ItemPickaxe) return "Pickaxe";
        if (item instanceof ItemAxe) return "Axe";
        if (item == Items.golden_apple) return "Gapple";
        if (item instanceof ItemFood) return "Food";
        if (item instanceof ItemBlock) return "Block";
        if (item == Items.water_bucket || item == Items.lava_bucket) return "Water";
        if (item == Items.ender_pearl) return "Pearl";
        if (item == Items.egg || item == Items.snowball) return "Throwable";
        if (item == Items.potionitem) return isBadPotion(stack) ? "BadPotion" : "GoodPotion";
        return "Unknown";
    }

    public static double getItemScore(ItemStack stack) {
        if (stack == null) return -1;
        if (stack.getItem() instanceof ItemArmor) return getArmorScore(stack);
        if (stack.getItem() instanceof ItemSword) return getSwordScore(stack);
        if (stack.getItem() instanceof ItemBow) return getBowScore(stack);
        if (getToolScore(stack) > 0) return getToolScore(stack);
        return -1;
    }

    public static int getMaxDamage(ItemStack stack) {
        if (stack == null) return 0;
        return stack.getMaxDamage();
    }

    public static int getDamage(ItemStack stack) {
        if (stack == null) return 0;
        return stack.getItemDamage();
    }

    public static double getArmorScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return -1;
        ItemArmor armor = (ItemArmor) stack.getItem();
        double score = armor.damageReduceAmount;
        score += (6 + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)) / 3.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, stack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, stack) / 11.0;
        return score;
    }

    public static double getSwordScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) return -1;
        ItemSword sword = (ItemSword) stack.getItem();
        double score = sword.getDamageVsEntity();
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.5;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 1.2;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack) * 0.8;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 10.0;
        return score;
    }

    public static double getBowScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemBow)) return -1;
        double score = 0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack) * 2.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack) * 1.5;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 1.2;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) * 3.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 10.0;
        return score;
    }

    public static double getToolScore(ItemStack stack) {
        if (stack == null) return -1;
        if (!(stack.getItem() instanceof ItemTool) && !(stack.getItem() instanceof ItemHoe)) return -1;
        double score = 0;
        if (stack.getItem() instanceof ItemTool) {
            ItemTool tool = (ItemTool) stack.getItem();
            score += tool.getToolMaterial().getEfficiencyOnProperMaterial();
        }
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 2.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 10.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack) / 10.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack) * 5.0;
        return score;
    }

    public static boolean isSameToolType(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.getItem() instanceof ItemPickaxe) return s2.getItem() instanceof ItemPickaxe;
        if (s1.getItem() instanceof ItemAxe) return s2.getItem() instanceof ItemAxe;
        if (s1.getItem() instanceof ItemSpade) return s2.getItem() instanceof ItemSpade;
        if (s1.getItem() instanceof ItemHoe) return s2.getItem() instanceof ItemHoe;
        return false;
    }
}