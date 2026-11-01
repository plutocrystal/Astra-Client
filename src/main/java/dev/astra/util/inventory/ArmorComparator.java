package dev.astra.util.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArmorComparator {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private ArmorComparator() {
    }

    public static class ArmorPiece {
        public final Integer index;
        public final ItemStack stack;
        public ArmorPiece(Integer index, ItemStack stack) {
            this.index = index;
            this.stack = stack;
        }
    }

    public static class ArmorSet {
        private final ArmorPiece[] armorPairs;
        public ArmorSet(ArmorPiece... armorPairs) {
            this.armorPairs = armorPairs;
        }
        public float getDefenseFactor() {
            int baseDefensePercentage = 0;
            int epf = 0;
            for (ArmorPiece pair : armorPairs) {
                if (pair == null || pair.stack == null) continue;
                ItemArmor item = (ItemArmor) pair.stack.getItem();
                baseDefensePercentage += item.getArmorMaterial().getDamageReductionAmount(item.armorType) * 4;
                int protectionLvl = StackExtensions.getEnchantmentLevel(pair.stack, Enchantment.protection);
                if (protectionLvl > 0) {
                    epf += (int) ((6 + protectionLvl * protectionLvl) * 0.75f / 3);
                }
            }
            float baseDefense = baseDefensePercentage / 100f;
            epf = Math.min(epf, 25);
            return baseDefense + (1 - baseDefense) * epf * 0.75f * 0.04f;
        }
        public boolean containsStack(ItemStack stack) {
            for (ArmorPiece pair : armorPairs) {
                if (pair != null && pair.stack == stack) return true;
            }
            return false;
        }
        public boolean containsIndex(int index) {
            for (ArmorPiece pair : armorPairs) {
                if (pair != null && pair.index != null && pair.index == index) return true;
            }
            return false;
        }
        public int indexOf(ItemStack stack) {
            for (ArmorPiece pair : armorPairs) {
                if (pair != null && pair.stack == stack) return pair.index == null ? -1 : pair.index;
            }
            return -1;
        }
        public ArmorPiece get(int index) {
            if (index < 0 || index >= armorPairs.length) return null;
            return armorPairs[index];
        }
    }

    public static ArmorSet getBestArmorSet(List<ItemStack> stacks) {
        return getBestArmorSet(stacks, null);
    }

    @SuppressWarnings("deprecation")
    public static ArmorSet getBestArmorSet(List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap) {
        if (mc.thePlayer == null) return null;
        List<ArmorPiece> all = new ArrayList<>();
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            for (ItemStack stack : entityStacksMap.keySet()) {
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    all.add(new ArmorPiece(-1, stack));
                }
            }
        }
        if (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0) {
            for (ItemStack stack : mc.thePlayer.inventory.armorInventory) {
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    all.add(new ArmorPiece(null, stack));
                }
            }
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                all.add(new ArmorPiece(i, stack));
            }
        }
        Comparator<ArmorPiece> comparator = Comparator.comparingDouble(piece -> {
            if (piece.index != null && piece.index == -1) {
                EntityItem entityItem = entityStacksMap == null ? null : entityStacksMap.get(piece.stack);
                if (entityItem != null) {
                    return mc.thePlayer.getDistanceSqToEntity(entityItem);
                }
                return -1.0D;
            }
            return -1.0D;
        });
        comparator = comparator.thenComparingInt(piece -> {
            if (isEquipped(piece.stack)) return Integer.MIN_VALUE;
            return piece.index == null ? Integer.MIN_VALUE : -piece.index;
        });
        comparator = comparator.thenComparingInt(piece -> {
            if (isEquipped(piece.stack)) return Integer.MAX_VALUE;
            return piece.index == null ? Integer.MAX_VALUE : piece.index;
        });
        comparator = comparator.thenComparingInt(piece -> -StackExtensions.totalDurability(piece.stack));
        comparator = comparator.thenComparingInt(piece -> -StackExtensions.enchantmentCount(piece.stack));
        comparator = comparator.thenComparingInt(piece -> -StackExtensions.enchantmentSum(piece.stack));
        all.sort(comparator);
        Map<Integer, List<ArmorPiece>> armorMap = new HashMap<>();
        for (ArmorPiece piece : all) {
            int type = ((ItemArmor) piece.stack.getItem()).armorType;
            armorMap.computeIfAbsent(type, k -> new ArrayList<>()).add(piece);
        }
        List<ArmorPiece> nullList = new ArrayList<>(1);
        nullList.add(null);
        List<ArmorPiece> helmets = armorMap.getOrDefault(0, nullList);
        List<ArmorPiece> chestplates = armorMap.getOrDefault(1, nullList);
        List<ArmorPiece> leggings = armorMap.getOrDefault(2, nullList);
        List<ArmorPiece> boots = armorMap.getOrDefault(3, nullList);
        ArmorSet best = null;
        float bestDefense = Float.NEGATIVE_INFINITY;
        for (ArmorPiece helmet : helmets) {
            for (ArmorPiece chestplate : chestplates) {
                for (ArmorPiece legging : leggings) {
                    for (ArmorPiece boot : boots) {
                        ArmorSet set = new ArmorSet(helmet, chestplate, legging, boot);
                        float defense = set.getDefenseFactor();
                        if (defense > bestDefense) {
                            bestDefense = defense;
                            best = set;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean isEquipped(ItemStack stack) {
        for (ItemStack equipped : mc.thePlayer.inventory.armorInventory) {
            if (equipped == stack) return true;
        }
        return false;
    }
}
