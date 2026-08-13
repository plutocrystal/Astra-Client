package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.ItemUtil;
import dev.astra.util.RandomUtils;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.*;

public class AutoArmor extends Module {
    private final ModeValue mode = new ModeValue("Mode", "OpenInv", "OpenInv", "SpoofInv", "Basic");
    private final BooleanValue noMove = new BooleanValue("NoMove", false);
    private final BooleanValue delayOnOpen = new BooleanValue("DelayOnOpen", true);
    public final BooleanValue hotbar = new BooleanValue("Hotbar", true);
    public final BooleanValue hotbarSilent = new BooleanValue("HotbarSilent", false);
    private final RangeSelectionNumberValue startDelay = new RangeSelectionNumberValue("StartDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue delay = new RangeSelectionNumberValue("Delay", 50.0, 100.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue hotbarSwitchDelay = new RangeSelectionNumberValue("HotbarSwitchDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue hotbarEquipDelay = new RangeSelectionNumberValue("HotbarEquipDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);
    private final RangeSelectionNumberValue hotbarBackDelay = new RangeSelectionNumberValue("HotbarBackDelay", 100.0, 200.0, 0.0, 1000.0, 1.0);

    private long currentDelay;
    private long lastEquipTime;
    private long guiOpenTime;
    private long currentStartDelay;
    private boolean spoofed;
    private boolean wasGuiOpen = false;
    private boolean firstAction = true;
    public boolean isActing = false;

    private int swapStep = 0;
    private int swapArmorWindow = -1;

    private int hotbarStep = 0;
    private List<Integer> hotbarQueue = new ArrayList<>();
    private int hotbarCurrentIdx = 0;
    private int hotbarPrevSlot = -1;
    private long hotbarNextTime = 0;

    public AutoArmor() {
        super("AutoArmor", 0, Category.PLAYER);
        addValues(mode, noMove, delayOnOpen, hotbar, hotbarSilent, startDelay, delay, hotbarSwitchDelay, hotbarEquipDelay, hotbarBackDelay);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastEquipTime = System.currentTimeMillis();
        guiOpenTime = System.currentTimeMillis();
        currentStartDelay = (long) RandomUtils.nextDouble(startDelay.getMinValue(), startDelay.getMaxValue());
        currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
        wasGuiOpen = false;
        firstAction = true;
        resetSwap();
        resetHotbar();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        closeSpoof();
        wasGuiOpen = false;
        isActing = false;
        resetSwap();
        resetHotbar();
    }

    private void resetSwap() {
        swapStep = 0;
        swapArmorWindow = -1;
    }

    private void resetHotbar() {
        hotbarStep = 0;
        hotbarQueue.clear();
        hotbarCurrentIdx = 0;
        hotbarPrevSlot = -1;
    }

    public boolean isBusy() {
        return this.isToggle() && (isActing || swapStep > 0 || hotbarStep > 0 || (mc.thePlayer != null && mc.thePlayer.inventory.getItemStack() != null));
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0);
    }

    private void updateActionTime() {
        lastEquipTime = System.currentTimeMillis();
        currentDelay = (long) RandomUtils.nextDouble(delay.getMinValue(), delay.getMaxValue());
        firstAction = false;
    }

    private int getDurability(ItemStack stack) {
        if (stack == null) return 0;
        return stack.getMaxDamage() - stack.getItemDamage();
    }

    private void switchToSlot(int slot) {
        if (hotbarSilent.isEnabled()) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
        } else {
            mc.thePlayer.inventory.currentItem = slot;
        }
    }

    private void switchBack() {
        if (hotbarPrevSlot != -1) {
            switchToSlot(hotbarPrevSlot);
        }
    }

    private void equipCurrentItem() {
        if (hotbarSilent.isEnabled()) {
            int slot = hotbarQueue.get(hotbarCurrentIdx);
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(stack));
        } else {
            mc.rightClickMouse();
        }
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer) return;

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

        isActing = false;

        if (hotbarStep > 0) {
            isActing = true;
            if (System.currentTimeMillis() < hotbarNextTime) return;

            if (hotbarStep == 1) {
                if (hotbarCurrentIdx >= hotbarQueue.size()) {
                    switchBack();
                    hotbarNextTime = System.currentTimeMillis() + (long) RandomUtils.nextDouble(hotbarBackDelay.getMinValue(), hotbarBackDelay.getMaxValue());
                    hotbarStep = 2;
                    return;
                }
                int targetSlot = hotbarQueue.get(hotbarCurrentIdx);
                ItemStack stackToEquip = mc.thePlayer.inventory.getStackInSlot(targetSlot);
                if (stackToEquip == null || !(stackToEquip.getItem() instanceof ItemArmor)) {
                    hotbarCurrentIdx++;
                    return;
                }
                ItemArmor armor = (ItemArmor) stackToEquip.getItem();
                int armorInvIndex = 3 - armor.armorType;
                if (mc.thePlayer.inventory.armorItemInSlot(armorInvIndex) != null) {
                    hotbarCurrentIdx++;
                    return;
                }
                switchToSlot(targetSlot);
                hotbarNextTime = System.currentTimeMillis() + (long) RandomUtils.nextDouble(hotbarSwitchDelay.getMinValue(), hotbarSwitchDelay.getMaxValue());
                hotbarStep = 2;
                return;
            } else if (hotbarStep == 2) {
                int targetSlot = hotbarQueue.get(hotbarCurrentIdx);
                ItemStack stackToEquip = mc.thePlayer.inventory.getStackInSlot(targetSlot);
                if (stackToEquip == null || !(stackToEquip.getItem() instanceof ItemArmor)) {
                    hotbarCurrentIdx++;
                    hotbarStep = 1;
                    return;
                }
                equipCurrentItem();
                hotbarNextTime = System.currentTimeMillis() + (long) RandomUtils.nextDouble(hotbarEquipDelay.getMinValue(), hotbarEquipDelay.getMaxValue());
                hotbarStep = 3;
                return;
            } else if (hotbarStep == 3) {
                hotbarCurrentIdx++;
                if (hotbarCurrentIdx < hotbarQueue.size()) {
                    int nextSlot = hotbarQueue.get(hotbarCurrentIdx);
                    switchToSlot(nextSlot);
                    hotbarNextTime = System.currentTimeMillis() + (long) RandomUtils.nextDouble(hotbarSwitchDelay.getMinValue(), hotbarSwitchDelay.getMaxValue());
                    hotbarStep = 2;
                } else {
                    switchBack();
                    hotbarNextTime = System.currentTimeMillis() + (long) RandomUtils.nextDouble(hotbarBackDelay.getMinValue(), hotbarBackDelay.getMaxValue());
                    hotbarStep = 4;
                }
                return;
            } else if (hotbarStep == 4) {
                resetHotbar();
                updateActionTime();
                return;
            }
        }

        if (swapStep > 0) {
            isActing = true;
            if (System.currentTimeMillis() - lastEquipTime < currentDelay) return;
            openSpoof();
            if (swapStep == 1) {
                mc.playerController.windowClick(0, swapArmorWindow, 0, 0, mc.thePlayer);
                swapStep = 2;
                updateActionTime();
                return;
            } else if (swapStep == 2) {
                mc.playerController.windowClick(0, -999, 0, 0, mc.thePlayer);
                resetSwap();
                updateActionTime();
                return;
            }
        }

        if (mc.thePlayer.inventory.getItemStack() != null) {
            isActing = true;
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
            if (System.currentTimeMillis() - lastEquipTime < currentDelay) return;
        }

        int bestSlot = -1;
        double bestScore = -1;
        int bestType = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemArmor)) continue;

            ItemArmor armor = (ItemArmor) stack.getItem();
            int type = armor.armorType;
            int armorInvIndex = 3 - type;

            ItemStack worn = mc.thePlayer.inventory.armorItemInSlot(armorInvIndex);
            double wornScore = ItemUtil.getArmorScore(worn);
            double itemScore = ItemUtil.getArmorScore(stack);

            boolean better;
            if (worn == null) {
                better = true;
            } else if (itemScore > wornScore) {
                better = true;
            } else if (itemScore == wornScore) {
                better = getDurability(stack) > getDurability(worn);
            } else {
                better = false;
            }

            if (better && itemScore > bestScore) {
                bestScore = itemScore;
                bestSlot = i;
                bestType = type;
            }
        }

        if (bestSlot != -1) {
            isActing = true;
            ItemArmor bestArmor = (ItemArmor) mc.thePlayer.inventory.getStackInSlot(bestSlot).getItem();
            int bestArmorInvIndex = 3 - bestArmor.armorType;
            boolean wornEmpty = mc.thePlayer.inventory.armorItemInSlot(bestArmorInvIndex) == null;

            if (hotbar.isEnabled() && bestSlot < 9 && wornEmpty && !(mc.currentScreen instanceof GuiInventory)) {
                hotbarQueue.clear();
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (stack == null || !(stack.getItem() instanceof ItemArmor)) continue;
                    ItemArmor armor = (ItemArmor) stack.getItem();
                    int armorInvIndex = 3 - armor.armorType;
                    if (mc.thePlayer.inventory.armorItemInSlot(armorInvIndex) != null) continue;
                    double itemScore = ItemUtil.getArmorScore(stack);
                    double wornScore = ItemUtil.getArmorScore(mc.thePlayer.inventory.armorItemInSlot(armorInvIndex));
                    if (itemScore > wornScore) {
                        hotbarQueue.add(i);
                    }
                }
                if (!hotbarQueue.isEmpty()) {
                    hotbarPrevSlot = mc.thePlayer.inventory.currentItem;
                    hotbarCurrentIdx = 0;
                    int firstSlot = hotbarQueue.get(0);
                    switchToSlot(firstSlot);
                    hotbarNextTime = System.currentTimeMillis() + (long) RandomUtils.nextDouble(hotbarSwitchDelay.getMinValue(), hotbarSwitchDelay.getMaxValue());
                    hotbarStep = 2;
                    return;
                }
            }

            if (mode.is("OpenInv") && !(mc.currentScreen instanceof GuiInventory)) {
                return;
            }

            openSpoof();
            int slotClick = bestSlot < 9 ? bestSlot + 36 : bestSlot;
            int armorWindowSlot = 5 + bestType;

            mc.playerController.windowClick(0, slotClick, 0, 0, mc.thePlayer);
            swapArmorWindow = armorWindowSlot;
            swapStep = 1;
            updateActionTime();
        } else {
            closeSpoof();
        }
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
