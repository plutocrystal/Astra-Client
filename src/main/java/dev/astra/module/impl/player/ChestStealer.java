package dev.astra.module.impl.player;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.ItemUtil;
import dev.astra.util.RandomUtils;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;

import java.util.ArrayList;
import java.util.List;

public class ChestStealer extends Module {
    private final RangeSelectionNumberValue delay = new RangeSelectionNumberValue("Delay", 50.0, 100.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue openDelay = new RangeSelectionNumberValue("OpenDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue autoCloseDelay = new RangeSelectionNumberValue("AutoCloseDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);
    private final BooleanValue random = new BooleanValue("Random", false);
    private final BooleanValue delayOnOpen = new BooleanValue("DelayOnOpen", true);
    private final BooleanValue stealBadPotion = new BooleanValue("StealBadPotion", false);
    private final BooleanValue smartSort = new BooleanValue("SmartSort", true);

    private long currentDelay;
    private long lastStealTime;
    private long openTime;
    private long emptyTime;
    private boolean wasOpen;
    private long currentOpenDelay;
    private long currentAutoCloseDelay;
    private Slot currentTargetSlot;
    private boolean firstAction;

    public ChestStealer() {
        super("ChestStealer", 0, Category.PLAYER);
        addValues(delay, openDelay, autoCloseDelay, random, delayOnOpen, stealBadPotion, smartSort);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        openTime = 0;
        emptyTime = 0;
        lastStealTime = 0;
        currentDelay = 0;
        currentOpenDelay = 0;
        currentAutoCloseDelay = 0;
        wasOpen = false;
        currentTargetSlot = null;
        firstAction = true;
    }

    private String getSortType(int slot) {
        if (Main.moduleManager == null) return "None";
        InvManager inv = Main.moduleManager.getModuleByClass(InvManager.class);
        if (inv == null || !inv.isToggle()) return "None";
        return inv.getSortType(slot);
    }

    private int findSortSlot(ItemStack stack) {
        if (stack == null) return -1;
        String cat = ItemUtil.getCategory(stack);
        for (int i = 0; i < 9; i++) {
            if (getSortType(i).equals(cat)) return i;
        }
        return -1;
    }

    private int findTrashSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;
            Item item = stack.getItem();
            if (item instanceof ItemSpade) return i;
            if (!ItemUtil.isUseful(stack)) return i;
            if (item == Items.potionitem && ItemUtil.isBadPotion(stack)) return i;
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) return i;
        }
        return -1;
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.thePlayer.openContainer instanceof ContainerChest && mc.currentScreen instanceof GuiChest) {
            ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;

            if (!wasOpen) {
                openTime = System.currentTimeMillis();
                wasOpen = true;
                currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
                currentOpenDelay = (long) RandomUtils.nextDouble(openDelay.getMinValue(), openDelay.getMaxValue());
                currentAutoCloseDelay = (long) RandomUtils.nextDouble(autoCloseDelay.getMinValue(), autoCloseDelay.getMaxValue());
                lastStealTime = System.currentTimeMillis();
                firstAction = true;
            }

            if (System.currentTimeMillis() - openTime < currentOpenDelay) {
                return;
            }

            List<Slot> validSlots = new ArrayList<>();
            int chestSize = chest.getLowerChestInventory().getSizeInventory();
            for (int i = 0; i < chestSize; i++) {
                Slot slot = chest.getSlot(i);
                if (slot.getHasStack() && shouldSteal(slot.getStack())) {
                    validSlots.add(slot);
                }
            }

            if (validSlots.isEmpty()) {
                if (emptyTime == 0) {
                    emptyTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - emptyTime >= currentAutoCloseDelay) {
                    mc.thePlayer.closeScreen();
                    wasOpen = false;
                }
            } else {
                emptyTime = 0;

                if (currentTargetSlot == null || !currentTargetSlot.getHasStack() || !validSlots.contains(currentTargetSlot)) {
                    if (random.isEnabled()) {
                        RandomUtils.shuffle(validSlots);
                    }
                    currentTargetSlot = validSlots.get(0);
                }
                Slot targetSlot = currentTargetSlot;

                if (!delayOnOpen.isEnabled() || !firstAction) {
                    if (System.currentTimeMillis() - lastStealTime < currentDelay) return;
                }

                if (smartSort.isEnabled()) {
                    int hotbarSlot = findSortSlot(targetSlot.getStack());
                    if (hotbarSlot != -1) {
                        ItemStack current = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);
                        if (current == null) {
                            mc.playerController.windowClick(chest.windowId, targetSlot.slotNumber, hotbarSlot, 2, mc.thePlayer);
                            lastStealTime = System.currentTimeMillis();
                            currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
                            currentTargetSlot = null;
                            firstAction = false;
                            return;
                        }
                        if (ItemUtil.getCategory(current).equals(ItemUtil.getCategory(targetSlot.getStack()))) {
                            double score1 = ItemUtil.getItemScore(targetSlot.getStack());
                            double score2 = ItemUtil.getItemScore(current);
                            if (score1 > score2 || (score1 == score2 && (targetSlot.getStack().getMaxDamage() - targetSlot.getStack().getItemDamage()) > (current.getMaxDamage() - current.getItemDamage()))) {
                                mc.playerController.windowClick(chest.windowId, targetSlot.slotNumber, hotbarSlot, 2, mc.thePlayer);
                                lastStealTime = System.currentTimeMillis();
                                currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
                                currentTargetSlot = null;
                                firstAction = false;
                                return;
                            }
                        }
                    }

                    AutoArmor autoArmor = Main.moduleManager.getModuleByClass(AutoArmor.class);
                    if (autoArmor != null && autoArmor.isToggle() && autoArmor.hotbar.isEnabled()) {
                        ItemStack chestStack = targetSlot.getStack();
                        if (chestStack.getItem() instanceof ItemArmor) {
                            int armorType = ((ItemArmor) chestStack.getItem()).armorType;
                            int armorInvIndex = 3 - armorType;
                            ItemStack worn = mc.thePlayer.inventory.armorItemInSlot(armorInvIndex);
                            if (worn == null || ItemUtil.getArmorScore(chestStack) > ItemUtil.getArmorScore(worn) ||
                                (ItemUtil.getArmorScore(chestStack) == ItemUtil.getArmorScore(worn) &&
                                 (chestStack.getMaxDamage() - chestStack.getItemDamage()) > (worn.getMaxDamage() - worn.getItemDamage()))) {
                                int trashSlot = findTrashSlot();
                                if (trashSlot != -1) {
                                    mc.playerController.windowClick(chest.windowId, targetSlot.slotNumber, trashSlot, 2, mc.thePlayer);
                                    lastStealTime = System.currentTimeMillis();
                                    currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
                                    currentTargetSlot = null;
                                    firstAction = false;
                                    return;
                                }
                                int emptySlot = findEmptyHotbarSlot();
                                if (emptySlot != -1) {
                                    mc.playerController.windowClick(chest.windowId, targetSlot.slotNumber, emptySlot, 2, mc.thePlayer);
                                    lastStealTime = System.currentTimeMillis();
                                    currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
                                    currentTargetSlot = null;
                                    firstAction = false;
                                    return;
                                }
                            }
                        }
                    }
                }

                mc.playerController.windowClick(chest.windowId, targetSlot.slotNumber, 0, 1, mc.thePlayer);
                lastStealTime = System.currentTimeMillis();
                currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
                currentTargetSlot = null;
                firstAction = false;
            }
        } else {
            resetState();
        }
    }

    private boolean shouldSteal(ItemStack stack) {
        if (!ItemUtil.isUseful(stack)) return false;

        Item item = stack.getItem();
        if (item instanceof ItemSpade) return false;
        if (item == Items.potionitem && ItemUtil.isBadPotion(stack) && !stealBadPotion.isEnabled()) return false;

        if (item instanceof ItemArmor || item instanceof ItemSword || item instanceof ItemBow || ItemUtil.getToolScore(stack) > 0) {
            if (item instanceof ItemArmor) {
                ItemStack worn = mc.thePlayer.inventory.armorItemInSlot(3 - ((ItemArmor) item).armorType);
                if (worn != null) {
                    double score1 = ItemUtil.getItemScore(stack);
                    double score2 = ItemUtil.getItemScore(worn);
                    if (score1 < score2) return false;
                    if (score1 == score2) {
                        int dura1 = ItemUtil.getMaxDamage(stack) - ItemUtil.getDamage(stack);
                        int dura2 = ItemUtil.getMaxDamage(worn) - ItemUtil.getDamage(worn);
                        if (dura1 <= dura2) return false;
                    }
                }
            }

            for (int i = 0; i < 36; i++) {
                ItemStack invStack = mc.thePlayer.inventory.getStackInSlot(i);
                if (invStack != null) {
                    boolean sameType = false;
                    if (item instanceof ItemArmor && invStack.getItem() instanceof ItemArmor) {
                        if (((ItemArmor) item).armorType == ((ItemArmor) invStack.getItem()).armorType) sameType = true;
                    } else if (item instanceof ItemSword && invStack.getItem() instanceof ItemSword) {
                        sameType = true;
                    } else if (item instanceof ItemBow && invStack.getItem() instanceof ItemBow) {
                        sameType = true;
                    } else if (ItemUtil.isSameToolType(stack, invStack)) {
                        sameType = true;
                    }

                    if (sameType) {
                        double score1 = ItemUtil.getItemScore(stack);
                        double score2 = ItemUtil.getItemScore(invStack);
                        if (score1 < score2) return false;
                        if (score1 == score2) {
                            int dura1 = ItemUtil.getMaxDamage(stack) - ItemUtil.getDamage(stack);
                            int dura2 = ItemUtil.getMaxDamage(invStack) - ItemUtil.getDamage(invStack);
                            if (dura1 <= dura2) return false;
                        }
                    }
                }
            }
        }

        InvManager invManager = Main.moduleManager.getModuleByClass(InvManager.class);
        if (invManager != null && invManager.isToggle()) {
            int currentCount = invManager.countItem(item);
            if (invManager.isLimitExceeded(item, currentCount)) return false;
        }

        return true;
    }
}
