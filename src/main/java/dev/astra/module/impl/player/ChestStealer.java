package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class ChestStealer extends Module {

    public final NumberValue startDelay = new NumberValue("StartDelay", 200.0, 0.0, 1000.0, 1.0);
    public final NumberValue delay = new NumberValue("Delay", 100.0, 0.0, 400.0, 1.0);
    public final BooleanValue random = new BooleanValue("Random", false);
    public final BooleanValue intelligent = new BooleanValue("Intelligent", true);
    public final BooleanValue autoClose = new BooleanValue("AutoClose", true);
    public final BooleanValue spamClick = new BooleanValue("SpamClick", true);

    private long lastSteal = 0L;
    private long lastStart = 0L;
    private int lastItemPos = Integer.MIN_VALUE;

    public ChestStealer() {
        super("ChestStealer", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(startDelay, delay, random, intelligent, autoClose, spamClick);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.currentScreen instanceof GuiChest) {
            if (System.currentTimeMillis() - lastStart >= startDelay.getValue().longValue() + ThreadLocalRandom.current().nextLong(-35, 35)) {
                ArrayList<Integer> itemPos = new ArrayList<>();
                GuiChest chest = (GuiChest) mc.currentScreen;
                for (int i = 0; i < chest.inventorySlots.inventorySlots.size() - 36; i++) {
                    ItemStack itemStack = chest.inventorySlots.getSlot(i).getStack();
                    if (itemStack != null) {
                        if (intelligent.getValue()) {
                            if (isBestChestItem(itemStack) && isBestItem(itemStack)) {
                                itemPos.add(i);
                            }
                        } else {
                            itemPos.add(i);
                        }
                    }
                }
                if (random.getValue()) {
                    Collections.shuffle(itemPos);
                }
                if (System.currentTimeMillis() - lastSteal >= delay.getValue().longValue() + ThreadLocalRandom.current().nextLong(-35, 55)) {
                    boolean found = false;
                    for (Integer pos : itemPos) {
                        stealItem(pos);
                        lastItemPos = pos;
                        found = true;
                        if (delay.getValue() != 0.0) break;
                    }
                    if (!found && autoClose.getValue()) {
                        lastStart = System.currentTimeMillis();
                        mc.thePlayer.closeScreen();
                    }
                } else if (lastItemPos != Integer.MIN_VALUE && spamClick.getValue()) {
                    mc.playerController.windowClick(chest.inventorySlots.windowId, lastItemPos, 0, 1, mc.thePlayer);
                }
            }
        } else {
            lastStart = System.currentTimeMillis();
            lastItemPos = Integer.MIN_VALUE;
        }
    }

    public boolean isBestChestItem(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemSword || itemStack.getItem() instanceof ItemBow || itemStack.getItem() instanceof ItemArmor || itemStack.getItem() instanceof ItemAxe || itemStack.getItem() instanceof ItemPickaxe || itemStack.getItem() instanceof ItemSpade || itemStack.getItem() instanceof ItemFishingRod) {
            GuiChest chest = (GuiChest) mc.currentScreen;
            for (int i = 0; i < chest.inventorySlots.inventorySlots.size() - 36; i++) {
                ItemStack chestItem = chest.inventorySlots.getSlot(i).getStack();
                if (chestItem != null) {
                    if (itemStack.getItem() instanceof ItemSword && chestItem.getItem() instanceof ItemSword) {
                        if (getDamageSword(itemStack) < getDamageSword(chestItem)) return false;
                    } else if (itemStack.getItem() instanceof ItemBow && chestItem.getItem() instanceof ItemBow) {
                        if (getDamageBow(itemStack) < getDamageBow(chestItem)) return false;
                    } else if (itemStack.getItem() instanceof ItemArmor && chestItem.getItem() instanceof ItemArmor) {
                        if (((ItemArmor) itemStack.getItem()).armorType == ((ItemArmor) chestItem.getItem()).armorType && getDamageReduceAmount(itemStack) < getDamageReduceAmount(chestItem))
                            return false;
                    } else if (itemStack.getItem() instanceof ItemFishingRod && chestItem.getItem() instanceof ItemFishingRod) {
                        if (getBestRod(itemStack) < getBestRod(chestItem)) return false;
                    } else if (itemStack.getItem() instanceof ItemTool && chestItem.getItem() instanceof ItemTool && getToolSpeed(itemStack) < getToolSpeed(chestItem)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isBestItem(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemSword || itemStack.getItem() instanceof ItemBow || itemStack.getItem() instanceof ItemArmor || itemStack.getItem() instanceof ItemAxe || itemStack.getItem() instanceof ItemPickaxe || itemStack.getItem() instanceof ItemSpade || itemStack.getItem() instanceof ItemFishingRod) {
            for (int i = 0; i < mc.thePlayer.inventoryContainer.inventorySlots.size(); i++) {
                ItemStack inventoryStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (inventoryStack != null) {
                    if (itemStack.getItem() instanceof ItemSword && inventoryStack.getItem() instanceof ItemSword) {
                        if (getDamageSword(itemStack) <= getDamageSword(inventoryStack)) return false;
                    } else if (itemStack.getItem() instanceof ItemBow && inventoryStack.getItem() instanceof ItemBow) {
                        if (getDamageBow(itemStack) <= getDamageBow(inventoryStack)) return false;
                    } else if (itemStack.getItem() instanceof ItemArmor && inventoryStack.getItem() instanceof ItemArmor) {
                        if (((ItemArmor) itemStack.getItem()).armorType == ((ItemArmor) inventoryStack.getItem()).armorType && getDamageReduceAmount(itemStack) <= getDamageReduceAmount(inventoryStack))
                            return false;
                    } else if (itemStack.getItem() instanceof ItemFishingRod && inventoryStack.getItem() instanceof ItemFishingRod) {
                        if (getBestRod(itemStack) <= getBestRod(inventoryStack)) return false;
                    } else if (itemStack.getItem() instanceof ItemTool && inventoryStack.getItem() instanceof ItemTool && getToolSpeed(itemStack) <= getToolSpeed(inventoryStack)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void stealItem(int slot) {
        GuiChest chest = (GuiChest) mc.currentScreen;
        mc.playerController.windowClick(chest.inventorySlots.windowId, slot, 0, 1, mc.thePlayer);
        lastSteal = System.currentTimeMillis();
    }

    private double getDamageSword(ItemStack itemStack) {
        double damage = 0.0;
        if (itemStack.getItem() instanceof ItemSword) {
            damage += ((ItemSword) itemStack.getItem()).getDamageVsEntity() + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25f;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damage -= itemStack.getItemDamage() / 10000.0;
        }
        return damage;
    }

    private double getDamageBow(ItemStack itemStack) {
        double damage = 0.0;
        if (itemStack.getItem() instanceof ItemBow) {
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, itemStack) / 8.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack) / 8.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damage -= itemStack.getItemDamage() / 10000.0;
        }
        return damage;
    }

    private double getToolSpeed(ItemStack itemStack) {
        double damage = 0.0;
        if (itemStack.getItem() instanceof ItemTool) {
            if (itemStack.getItem() instanceof ItemAxe) {
                damage += itemStack.getItem().getStrVsBlock(itemStack, new Block(Material.wood, MapColor.woodColor)) + EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            } else if (itemStack.getItem() instanceof ItemPickaxe) {
                damage += itemStack.getItem().getStrVsBlock(itemStack, new Block(Material.rock, MapColor.stoneColor)) + EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            } else if (itemStack.getItem() instanceof ItemSpade) {
                damage += itemStack.getItem().getStrVsBlock(itemStack, new Block(Material.sand, MapColor.sandColor)) + EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            }
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) / 33.0;
            damage -= itemStack.getItemDamage() / 10000.0;
        }
        return damage;
    }

    private double getDamageReduceAmount(ItemStack itemStack) {
        double damageReduceAmount = 0.0;
        if (itemStack.getItem() instanceof ItemArmor) {
            damageReduceAmount += ((ItemArmor) itemStack.getItem()).damageReduceAmount + (6 + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack)) / 3.0f;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, itemStack) / 11.0;
            if (((ItemArmor) itemStack.getItem()).armorType == 0 && ((ItemArmor) itemStack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD) {
                damageReduceAmount -= 0.01;
            }
            damageReduceAmount -= itemStack.getItemDamage() / 10000.0;
        }
        return damageReduceAmount;
    }

    private double getBestRod(ItemStack itemStack) {
        double damage = 0.0;
        if (itemStack.getItem() instanceof ItemFishingRod) {
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.lure.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.luckOfTheSea.effectId, itemStack) / 33.0;
            damage -= itemStack.getItemDamage() / 10000.0;
        }
        return damage;
    }
}