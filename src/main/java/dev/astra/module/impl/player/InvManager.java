package dev.astra.module.impl.player;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.ItemUtil;
import dev.astra.util.RandomUtils;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

public class InvManager extends Module {
    private final ModeValue mode = new ModeValue("Mode", "OpenInv", "OpenInv", "SpoofInv", "Basic");
    private final BooleanValue noMove = new BooleanValue("NoMove", false);
    private final BooleanValue delayOnOpen = new BooleanValue("DelayOnOpen", true);
    private final RangeSelectionNumberValue startDelay = new RangeSelectionNumberValue("StartDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue delay = new RangeSelectionNumberValue("Delay", 50.0, 100.0, 0.0, 1000.0, 1.0);
    private final BooleanValue keepBadPotion = new BooleanValue("KeepBadPotion", false);

    private final String[] sortItems = {"None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl", "Throwable", "GoodPotion", "BadPotion"};
    private final ModeValue sortSlot1 = new ModeValue("SortSlot-1", "Sword", sortItems);
    private final ModeValue sortSlot2 = new ModeValue("SortSlot-2", "Bow", sortItems);
    private final ModeValue sortSlot3 = new ModeValue("SortSlot-3", "Pickaxe", sortItems);
    private final ModeValue sortSlot4 = new ModeValue("SortSlot-4", "Axe", sortItems);
    private final ModeValue sortSlot5 = new ModeValue("SortSlot-5", "None", sortItems);
    private final ModeValue sortSlot6 = new ModeValue("SortSlot-6", "None", sortItems);
    private final ModeValue sortSlot7 = new ModeValue("SortSlot-7", "Food", sortItems);
    private final ModeValue sortSlot8 = new ModeValue("SortSlot-8", "Block", sortItems);
    private final ModeValue sortSlot9 = new ModeValue("SortSlot-9", "Block", sortItems);

    public final NumberValue blockLimit = new NumberValue("BlockLimit", 512, 0, 1000, 1);
    public final NumberValue foodLimit = new NumberValue("FoodLimit", 64, 0, 1000, 1);
    public final NumberValue arrowLimit = new NumberValue("ArrowLimit", 256, 0, 1000, 1);
    public final NumberValue throwableLimit = new NumberValue("ThrowableLimit", 64, 0, 1000, 1);
    public final NumberValue bucketLimit = new NumberValue("BucketLimit", 2, 0, 100, 1);
    public final NumberValue potionLimit = new NumberValue("PotionLimit", 8, 0, 1000, 1);
    public final NumberValue rodLimit = new NumberValue("RodLimit", 1, 0, 1000, 1);

    private long currentDelay;
    private long lastActionTime;
    private long guiOpenTime;
    private long currentStartDelay;
    private boolean spoofed;
    private boolean wasGuiOpen = false;
    private boolean firstAction = true;

    public InvManager() {
        super("InvManager", 0, Category.PLAYER);
        addValues(mode, noMove, delayOnOpen, startDelay, delay, keepBadPotion, sortSlot1, sortSlot2, sortSlot3, sortSlot4, sortSlot5, sortSlot6, sortSlot7, sortSlot8, sortSlot9,
                blockLimit, foodLimit, arrowLimit, throwableLimit, bucketLimit, potionLimit, rodLimit);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastActionTime = System.currentTimeMillis();
        guiOpenTime = System.currentTimeMillis();
        currentStartDelay = (long) RandomUtils.nextDouble(startDelay.getMinValue(), startDelay.getMaxValue());
        currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
        wasGuiOpen = false;
        firstAction = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        closeSpoof();
        wasGuiOpen = false;
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0);
    }

    private void updateActionTime() {
        lastActionTime = System.currentTimeMillis();
        currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
        firstAction = false;
    }

    private boolean isWorseOrEqual(ItemStack a, ItemStack b) {
        double scoreA = ItemUtil.getItemScore(a);
        double scoreB = ItemUtil.getItemScore(b);
        if (scoreB > scoreA) return true;
        if (scoreB == scoreA) {
            int duraA = ItemUtil.getMaxDamage(a) - ItemUtil.getDamage(a);
            int duraB = ItemUtil.getMaxDamage(b) - ItemUtil.getDamage(b);
            return duraB >= duraA;
        }
        return false;
    }

    public String getSortType(int slot) {
        switch (slot) {
            case 0: return sortSlot1.getValue();
            case 1: return sortSlot2.getValue();
            case 2: return sortSlot3.getValue();
            case 3: return sortSlot4.getValue();
            case 4: return sortSlot5.getValue();
            case 5: return sortSlot6.getValue();
            case 6: return sortSlot7.getValue();
            case 7: return sortSlot8.getValue();
            case 8: return sortSlot9.getValue();
            default: return "";
        }
    }

    public boolean isLimitExceeded(Item item, int total) {
        if (item instanceof ItemBlock && total > blockLimit.getValue()) return true;
        if (item instanceof ItemFood && item != Items.golden_apple && total > foodLimit.getValue()) return true;
        if (item == Items.arrow && total > arrowLimit.getValue()) return true;
        if ((item == Items.egg || item == Items.snowball) && total > throwableLimit.getValue()) return true;
        if ((item == Items.water_bucket || item == Items.lava_bucket) && total > bucketLimit.getValue()) return true;
        if (item == Items.potionitem && total > potionLimit.getValue()) return true;
        if (item == Items.fishing_rod && total > rodLimit.getValue()) return true;
        return false;
    }

    public int countItem(Item item) {
        int total = 0;
        for (int j = 0; j < 36; j++) {
            ItemStack s2 = mc.thePlayer.inventory.getStackInSlot(j);
            if (s2 != null) {
                if (item instanceof ItemBlock && s2.getItem() instanceof ItemBlock) total += s2.stackSize;
                else if (item instanceof ItemFood && item != Items.golden_apple && s2.getItem() instanceof ItemFood && s2.getItem() != Items.golden_apple) total += s2.stackSize;
                else if (item == Items.arrow && s2.getItem() == Items.arrow) total += s2.stackSize;
                else if ((item == Items.egg || item == Items.snowball) &&
                         (s2.getItem() == Items.egg || s2.getItem() == Items.snowball)) total += s2.stackSize;
                else if ((item == Items.water_bucket || item == Items.lava_bucket) &&
                         (s2.getItem() == Items.water_bucket || s2.getItem() == Items.lava_bucket)) total += s2.stackSize;
                else if (item == Items.potionitem && s2.getItem() == Items.potionitem) total += s2.stackSize;
                else if (item == Items.fishing_rod && s2.getItem() == Items.fishing_rod) total += s2.stackSize;
            }
        }
        return total;
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer) return;

        AutoArmor autoArmor = Main.moduleManager.getModuleByClass(AutoArmor.class);
        if (autoArmor != null && autoArmor.isBusy()) {
            return;
        }

        if (noMove.isEnabled() && isMoving() && !(mc.currentScreen instanceof GuiInventory)) {
            closeSpoof();
            return;
        }

        boolean isGuiOpen = mc.currentScreen instanceof GuiInventory || (mode.is("SpoofInv") && spoofed);
        if (isGuiOpen && !wasGuiOpen) {
            guiOpenTime = System.currentTimeMillis();
            currentStartDelay = (long) RandomUtils.nextDouble(startDelay.getMinValue(), startDelay.getMaxValue());
            wasGuiOpen = true;
            firstAction = true;
        } else if (!isGuiOpen && wasGuiOpen) {
            wasGuiOpen = false;
        }

        if (mode.is("OpenInv") || mode.is("SpoofInv")) {
            if (System.currentTimeMillis() - guiOpenTime < currentStartDelay) {
                return;
            }
        }

        if (mc.thePlayer.inventory.getItemStack() != null) {
            for (int i = 0; i < 36; i++) {
                if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                    int slotClick = i < 9 ? i + 36 : i;
                    mc.playerController.windowClick(0, slotClick, 0, 0, mc.thePlayer);
                    updateActionTime();
                    return;
                }
            }
            mc.playerController.windowClick(0, -999, 0, 0, mc.thePlayer);
            updateActionTime();
            return;
        }

        if (!delayOnOpen.isEnabled() || !firstAction) {
            if (System.currentTimeMillis() - lastActionTime < currentDelay) return;
        }

        if (mode.is("OpenInv") && !(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        int throwSlot = -1;
        for (int i = 0; i < 36 && throwSlot == -1; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;
            Item item = stack.getItem();

            if (item instanceof ItemSpade) {
                throwSlot = i;
                break;
            }

            if (!ItemUtil.isUseful(stack)) {
                throwSlot = i;
                break;
            }

            if (item == Items.potionitem && ItemUtil.isBadPotion(stack) && !keepBadPotion.isEnabled()) {
                throwSlot = i;
                break;
            }

            int total = countItem(item);
            if (isLimitExceeded(item, total)) {
                throwSlot = i;
                break;
            }

            if (item instanceof ItemArmor || item instanceof ItemSword || item instanceof ItemBow || ItemUtil.getToolScore(stack) > 0) {
                boolean shouldThrow = false;

                if (item instanceof ItemArmor) {
                    ItemStack worn = mc.thePlayer.inventory.armorItemInSlot(3 - ((ItemArmor) item).armorType);
                    if (worn != null && isWorseOrEqual(stack, worn)) {
                        shouldThrow = true;
                    }
                }

                if (!shouldThrow) {
                    for (int j = 0; j < 36; j++) {
                        if (i == j) continue;
                        ItemStack s2 = mc.thePlayer.inventory.getStackInSlot(j);
                        if (s2 == null) continue;

                        boolean sameType = false;
                        if (item instanceof ItemArmor && s2.getItem() instanceof ItemArmor) {
                            if (((ItemArmor) item).armorType == ((ItemArmor) s2.getItem()).armorType) sameType = true;
                        } else if (item instanceof ItemSword && s2.getItem() instanceof ItemSword) {
                            sameType = true;
                        } else if (item instanceof ItemBow && s2.getItem() instanceof ItemBow) {
                            sameType = true;
                        } else if (ItemUtil.isSameToolType(stack, s2)) {
                            sameType = true;
                        }

                        if (sameType) {
                            double score1 = ItemUtil.getItemScore(stack);
                            double score2 = ItemUtil.getItemScore(s2);
                            if (score2 > score1) {
                                shouldThrow = true;
                                break;
                            } else if (score2 == score1) {
                                int dura1 = ItemUtil.getMaxDamage(stack) - ItemUtil.getDamage(stack);
                                int dura2 = ItemUtil.getMaxDamage(s2) - ItemUtil.getDamage(s2);
                                if (dura2 > dura1) {
                                    shouldThrow = true;
                                    break;
                                } else if (dura2 == dura1) {
                                    if (j < i) {
                                        shouldThrow = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (shouldThrow) {
                    throwSlot = i;
                    break;
                }
            }
        }

        if (throwSlot != -1) {
            openSpoof();
            int slotClick = throwSlot < 9 ? throwSlot + 36 : throwSlot;
            mc.playerController.windowClick(0, slotClick, 1, 4, mc.thePlayer);
            updateActionTime();
            return;
        }

        for (int i = 0; i < 9; i++) {
            String desired = getSortType(i);
            if (desired.equals("Ignore") || desired.equals("None")) continue;

            ItemStack current = mc.thePlayer.inventory.getStackInSlot(i);
            String currentCat = ItemUtil.getCategory(current);

            if (!currentCat.equals(desired)) {
                int foundSlot = -1;
                for (int j = 9; j < 36; j++) {
                    ItemStack s = mc.thePlayer.inventory.getStackInSlot(j);
                    if (s == null) continue;
                    if (ItemUtil.getCategory(s).equals(desired)) {
                        foundSlot = j;
                        break;
                    }
                }
                if (foundSlot != -1) {
                    openSpoof();
                    int slot2 = foundSlot < 9 ? foundSlot + 36 : foundSlot;
                    mc.playerController.windowClick(0, slot2, i, 2, mc.thePlayer);
                    updateActionTime();
                    return;
                }
            }
        }

        int fromSlot = -1, toSlot = -1;
        for (int i = 0; i < 36 && fromSlot == -1; i++) {
            ItemStack s1 = mc.thePlayer.inventory.getStackInSlot(i);
            if (s1 == null || s1.stackSize >= s1.getMaxStackSize()) continue;
            for (int j = i + 1; j < 36; j++) {
                ItemStack s2 = mc.thePlayer.inventory.getStackInSlot(j);
                if (s2 != null && s1.isItemEqual(s2) && ItemStack.areItemStackTagsEqual(s1, s2) && s2.stackSize < s2.getMaxStackSize()) {
                    fromSlot = j;
                    toSlot = i;
                    break;
                }
            }
        }

        if (fromSlot != -1 && toSlot != -1) {
            openSpoof();
            int fromClick = fromSlot < 9 ? fromSlot + 36 : fromSlot;
            int toClick = toSlot < 9 ? toSlot + 36 : toSlot;
            mc.playerController.windowClick(0, fromClick, 0, 0, mc.thePlayer);
            mc.playerController.windowClick(0, toClick, 0, 0, mc.thePlayer);
            updateActionTime();
            return;
        }

        closeSpoof();
    }

    private void openSpoof() {
        if (mode.is("SpoofInv") && !spoofed && mc.currentScreen == null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            spoofed = true;
            guiOpenTime = System.currentTimeMillis();
            currentStartDelay = (long) RandomUtils.nextDouble(startDelay.getMinValue(), startDelay.getMaxValue());
            wasGuiOpen = true;
            firstAction = true;
        }
    }

    private void closeSpoof() {
        if (mode.is("SpoofInv") && spoofed && mc.currentScreen == null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            spoofed = false;
            wasGuiOpen = false;
        }
    }
}
