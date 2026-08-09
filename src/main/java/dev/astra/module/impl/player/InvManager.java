package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InvManager extends Module {

    public final ModeValue mode = new ModeValue("Mode", "OpenInv", "OpenInv", "SpoofInv", "Basic");
    public final BooleanValue noMove = new BooleanValue("NoMove", false);
    public final BooleanValue interactionCheck = new BooleanValue("InteractCheck", false);
    public final RangeSelectionNumberValue delay = new RangeSelectionNumberValue("Delay", 50.0, 150.0, 0.0, 1000.0, 1.0);
    public final BooleanValue sort = new BooleanValue("Sort", true);

    private final String[] itemTypes = {"None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl", "Throwable", "GoodPotion", "BadPotion"};
    public final ModeValue sortSlot1 = new ModeValue("SortSlot-1", "Sword", itemTypes);
    public final ModeValue sortSlot2 = new ModeValue("SortSlot-2", "Bow", itemTypes);
    public final ModeValue sortSlot3 = new ModeValue("SortSlot-3", "Pickaxe", itemTypes);
    public final ModeValue sortSlot4 = new ModeValue("SortSlot-4", "Axe", itemTypes);
    public final ModeValue sortSlot5 = new ModeValue("SortSlot-5", "None", itemTypes);
    public final ModeValue sortSlot6 = new ModeValue("SortSlot-6", "None", itemTypes);
    public final ModeValue sortSlot7 = new ModeValue("SortSlot-7", "Food", itemTypes);
    public final ModeValue sortSlot8 = new ModeValue("SortSlot-8", "Block", itemTypes);
    public final ModeValue sortSlot9 = new ModeValue("SortSlot-9", "Block", itemTypes);

    private long lastAction = 0L;
    private boolean serverInvOpen = false;
    private boolean blockInv = false;

    public InvManager() {
        super("InvManager", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(mode, noMove, interactionCheck, delay, sort, 
                  sortSlot1, sortSlot2, sortSlot3, sortSlot4, sortSlot5, sortSlot6, sortSlot7, sortSlot8, sortSlot9);
    }

    @Override
    public void onDisable() {
        closeInv();
        super.onDisable();
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        boolean isMoving = mc.thePlayer.moveStrafing != 0 || mc.thePlayer.moveForward != 0;
        boolean invCleaner = mc.currentScreen instanceof GuiInventory || (!mode.is("OpenInv") && (mc.currentScreen == null || mc.currentScreen instanceof GuiInventory) && (!noMove.getValue() || (noMove.getValue() && !isMoving)));

        if (invCleaner && !blockInv) {
            long min = (long) delay.getMinValue();
            long max = (long) delay.getMaxValue();
            if (max < min) max = min;
            long currentDelay = ThreadLocalRandom.current().nextLong(min, max + 1);
            
            if (System.currentTimeMillis() - lastAction >= currentDelay) {
                openInv();
                if (sort.getValue() && handleSort()) {
                    lastAction = System.currentTimeMillis();
                    return;
                }
                if (handleDrop()) {
                    lastAction = System.currentTimeMillis();
                    return;
                }
            }
        } else {
            closeInv();
        }
    }

    private boolean handleSort() {
        ModeValue[] slots = {sortSlot1, sortSlot2, sortSlot3, sortSlot4, sortSlot5, sortSlot6, sortSlot7, sortSlot8, sortSlot9};
        for (int i = 0; i < 9; i++) {
            String target = slots[i].getValue();
            if (target.equals("Ignore")) continue;

            int slot = i + 36;
            ItemStack current = mc.thePlayer.openContainer.getSlot(slot).getStack();
            String currentType = getSortType(current);

            if (target.equals("None")) {
                if (current != null) {
                    mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 0, 1, mc.thePlayer);
                    return true;
                }
                continue;
            }

            int bestSlot = -1;
            double bestScore = -1;
            int bestSize = -1;

            for (int j = 9; j < 45; j++) {
                ItemStack invStack = mc.thePlayer.openContainer.getSlot(j).getStack();
                if (invStack == null) continue;
                String invType = getSortType(invStack);
                if (invType.equals(target)) {
                    if (target.equals("Sword") || target.equals("Pickaxe") || target.equals("Axe")) {
                        double score = target.equals("Sword") ? getDamageSword(invStack) : getToolSpeed(invStack);
                        if (score > bestScore) { bestScore = score; bestSlot = j; }
                    } else if (target.equals("Bow")) {
                        double score = -invStack.getItemDamage();
                        if (score > bestScore) { bestScore = score; bestSlot = j; }
                    } else {
                        int size = invStack.stackSize;
                        if (size > bestSize) { bestSize = size; bestSlot = j; }
                    }
                }
            }

            if (current != null && currentType.equals(target)) {
                if (target.equals("Sword")) {
                    if (getDamageSword(current) >= bestScore) continue;
                } else if (target.equals("Bow")) {
                    if (-current.getItemDamage() >= bestScore) continue;
                } else if (target.equals("Pickaxe") || target.equals("Axe")) {
                    if (getToolSpeed(current) >= bestScore) continue;
                } else {
                    if (current.stackSize >= bestSize) continue;
                }
            }

            if (bestSlot != -1) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, bestSlot, i, 2, mc.thePlayer);
                return true;
            }
        }
        return false;
    }

    private boolean handleDrop() {
        int bestSwordSlot = -1, bestPickaxeSlot = -1, bestAxeSlot = -1, bestBowSlot = -1;
        double bestSwordScore = -1, bestPickaxeScore = -1, bestAxeScore = -1, bestBowScore = -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.openContainer.getSlot(i).getStack();
            if (stack == null) continue;
            String type = getSortType(stack);
            if (type.equals("Sword")) {
                double score = getDamageSword(stack);
                if (score > bestSwordScore) { bestSwordScore = score; bestSwordSlot = i; }
            } else if (type.equals("Pickaxe")) {
                double score = getToolSpeed(stack);
                if (score > bestPickaxeScore) { bestPickaxeScore = score; bestPickaxeSlot = i; }
            } else if (type.equals("Axe")) {
                double score = getToolSpeed(stack);
                if (score > bestAxeScore) { bestAxeScore = score; bestAxeSlot = i; }
            } else if (type.equals("Bow")) {
                double score = -stack.getItemDamage();
                if (score > bestBowScore) { bestBowScore = score; bestBowSlot = i; }
            }
        }

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.openContainer.getSlot(i).getStack();
            if (stack == null) continue;
            String type = getSortType(stack);
            boolean drop = false;

            if (type.equals("Sword")) {
                if (i != bestSwordSlot) drop = true;
            } else if (type.equals("Bow")) {
                if (i != bestBowSlot) drop = true;
            } else if (type.equals("Pickaxe")) {
                if (i != bestPickaxeSlot) drop = true;
            } else if (type.equals("Axe")) {
                if (i != bestAxeSlot) drop = true;
            } else if (type.equals("Armor")) {
                int bestArmorSlot = -1;
                double bestArmorScore = -1;
                int bestArmorDurability = -1;
                int armorType = ((ItemArmor) stack.getItem()).armorType;
                
                for (int j = 9; j < 45; j++) {
                    ItemStack invStack = mc.thePlayer.openContainer.getSlot(j).getStack();
                    if (invStack != null && invStack.getItem() instanceof ItemArmor && ((ItemArmor) invStack.getItem()).armorType == armorType) {
                        double score = getArmorScore(invStack);
                        int durability = invStack.getMaxDamage() - invStack.getItemDamage();
                        if (score > bestArmorScore || (score == bestArmorScore && durability > bestArmorDurability)) {
                            bestArmorScore = score;
                            bestArmorDurability = durability;
                            bestArmorSlot = j;
                        }
                    }
                }
                
                ItemStack equippedStack = mc.thePlayer.openContainer.getSlot(5 + armorType).getStack();
                if (equippedStack != null && equippedStack.getItem() instanceof ItemArmor && ((ItemArmor) equippedStack.getItem()).armorType == armorType) {
                    double score = getArmorScore(equippedStack);
                    int durability = equippedStack.getMaxDamage() - equippedStack.getItemDamage();
                    if (score > bestArmorScore || (score == bestArmorScore && durability > bestArmorDurability)) {
                        bestArmorScore = score;
                        bestArmorDurability = durability;
                        bestArmorSlot = -1;
                    }
                }
                
                if (i != bestArmorSlot) {
                    drop = true;
                }
            } else if (type.equals("Other")) {
                drop = true;
            }

            if (drop) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, 1, 4, mc.thePlayer);
                return true;
            }
        }
        return false;
    }

    private String getSortType(ItemStack stack) {
        if (stack == null) return "None";
        Item item = stack.getItem();
        if (item instanceof ItemSword) return "Sword";
        if (item instanceof ItemBow) return "Bow";
        if (item instanceof ItemPickaxe) return "Pickaxe";
        if (item instanceof ItemAxe) return "Axe";
        if (item == Items.golden_apple) return "Gapple";
        if (item == Items.ender_pearl) return "Pearl";
        if (item == Items.water_bucket) return "Water";
        if (item == Items.snowball || item == Items.egg) return "Throwable";
        if (item == Items.arrow) return "Arrow";
        if (item instanceof ItemPotion) {
            ItemPotion potion = (ItemPotion) item;
            List<PotionEffect> effects = potion.getEffects(stack);
            if (effects != null && !effects.isEmpty()) {
                for (PotionEffect effect : effects) {
                    if (Potion.potionTypes[effect.getPotionID()].isBadEffect()) return "BadPotion";
                }
                return "GoodPotion";
            }
            return "BadPotion";
        }
        if (item instanceof ItemFood) return "Food";
        if (item instanceof ItemBlock) return "Block";
        if (item instanceof ItemArmor) return "Armor";
        return "Other";
    }

    private void closeInv() {
        if (serverInvOpen && mode.is("SpoofInv") && mc.currentScreen == null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            serverInvOpen = false;
        }
    }

    private void openInv() {
        if (mode.is("SpoofInv") && !serverInvOpen && mc.currentScreen == null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            serverInvOpen = true;
        }
    }

    private double getDamageSword(ItemStack itemStack) {
        double damage = 0.0;
        if (itemStack != null && itemStack.getItem() instanceof ItemSword) {
            damage += ((ItemSword) itemStack.getItem()).getDamageVsEntity() + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25f;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damage -= itemStack.getItemDamage() / 10000.0;
        }
        return damage;
    }

    private double getToolSpeed(ItemStack itemStack) {
        double damage = 0.0;
        if (itemStack != null && itemStack.getItem() instanceof ItemTool) {
            if (itemStack.getItem() instanceof ItemAxe) {
                damage += itemStack.getItem().getStrVsBlock(itemStack, new Block(Material.wood, MapColor.woodColor)) + EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            } else if (itemStack.getItem() instanceof ItemPickaxe) {
                damage += itemStack.getItem().getStrVsBlock(itemStack, new Block(Material.rock, MapColor.stoneColor)) + EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            }
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, itemStack) / 11.0;
            damage -= itemStack.getItemDamage() / 10000.0;
        }
        return damage;
    }

    private double getArmorScore(ItemStack itemStack) {
        if (itemStack == null || !(itemStack.getItem() instanceof ItemArmor)) return -1;
        double score = ((ItemArmor) itemStack.getItem()).damageReduceAmount;
        score += (6 + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack)) / 3.0f;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, itemStack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, itemStack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, itemStack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, itemStack) / 11.0;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, itemStack) / 11.0;
        return score;
    }
}