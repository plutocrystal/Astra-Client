package dev.astra.module.impl.player;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.RandomUtils;
import dev.astra.util.inventory.ArmorComparator;
import dev.astra.util.inventory.LBInventoryManager;
import dev.astra.util.inventory.LBInventoryUtils;
import dev.astra.util.inventory.SilentHotbar;
import dev.astra.util.inventory.StackExtensions;
import dev.astra.util.inventory.TickedActions;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import java.util.ArrayList;
import java.util.List;

public class AutoArmor extends Module {
    private final RangeSelectionNumberValue delay =
            new RangeSelectionNumberValue("Delay", 50.0, 50.0, 0.0, 1000.0, 1.0);

    private final NumberValue minItemAge = new NumberValue("MinItemAge", 0, 0, 2000, 1);

    public final BooleanValue invOpen = LBInventoryManager.invOpenValue;

    public final BooleanValue simulateInventory = LBInventoryManager.simulateInventoryValue;

    public final NumberValue postInventoryCloseDelay = LBInventoryManager.postInventoryCloseDelayValue;

    public final BooleanValue autoClose = LBInventoryManager.autoCloseValue;

    public final NumberValue startDelay = LBInventoryManager.startDelayValue;

    public final NumberValue closeDelay = LBInventoryManager.closeDelayValue;

    public final BooleanValue smartSwap = new BooleanValue("SmartSwap", true);

    public final BooleanValue noMove = LBInventoryManager.noMoveValue;

    public final BooleanValue noMoveAir = LBInventoryManager.noMoveAirValue;

    public final BooleanValue noMoveGround = LBInventoryManager.noMoveGroundValue;

    public final BooleanValue hotbar = new BooleanValue("Hotbar", true);

    public final BooleanValue delayedSlotSwitch = new BooleanValue("DelayedSlotSwitch", true);

    public final BooleanValue notInContainers = new BooleanValue("NotInContainers", false);

    public final BooleanValue highlightSlot = LBInventoryManager.highlightSlotValue;

    private static final int RES_ADVANCE = 0;

    private static final int RES_WAIT = 1;

    private static final int RES_ABORT = -1;

    private int clickStage = 0;

    private enum HotbarPhase { START, SCAN, FINAL_DELAY, AWAIT, RESET }

    private HotbarPhase hotbarPhase = HotbarPhase.START;

    private List<ItemStack> hotbarStacks = new ArrayList<>();

    private int hotbarCursor = 0;

    private boolean hasClickedHotbar = false;

    private ArmorComparator.ArmorSet hotbarBestSet = null;

    private enum InvPhase { START, SCAN_TYPE, RUN_SEQ, AWAIT }

    private InvPhase invPhase = InvPhase.START;

    private List<ItemStack> invStacks = new ArrayList<>();

    private int invTypeCursor = 0;

    private List<int[]> invSeq = new ArrayList<>();

    private int invSeqCursor = 0;

    public AutoArmor() {
        super("AutoArmor", 0, Category.PLAYER);
        addValues(delay, minItemAge,
                invOpen, simulateInventory, postInventoryCloseDelay, autoClose, startDelay, closeDelay,
                smartSwap, noMove, noMoveAir, noMoveGround, hotbar, delayedSlotSwitch, notInContainers,
                highlightSlot);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        TickedActions.clearTicked(this);
        resetFlows();
        SilentHotbar.resetSlot(this, false);
        LBInventoryManager.autoArmorCurrentSlot = -1;
        LBInventoryManager.autoArmorLastSlot = -1;
    }

    private void resetFlows() {
        hotbarPhase = HotbarPhase.START;
        invPhase = InvPhase.START;
        hasClickedHotbar = false;
        clickStage = 0;
        hotbarCursor = 0;
        invTypeCursor = 0;
        invSeqCursor = 0;
        invSeq = new ArrayList<>();
        hotbarBestSet = null;
    }

    public void refreshVisibility() {
        delayedSlotSwitch.setVisible(hotbar.isEnabled());
        notInContainers.setVisible(hotbar.isEnabled());
    }

    private boolean handleEvents() {
        return isToggle() && mc.thePlayer != null && mc.theWorld != null;
    }

    public boolean handleEventsPublic() {
        return handleEvents();
    }

    public boolean canEquipFromChest() {
        return handleEvents() && hotbar.isEnabled() && !notInContainers.isEnabled();
    }

    public void equipFromHotbarInChest(Integer hotbarIndex, ItemStack stack) {
        if (hotbarIndex == null || hotbarIndex < 0 || !canEquipFromChest()) {
            LBInventoryManager.autoArmorCurrentSlot = -1;
            LBInventoryManager.autoArmorLastSlot = -1;
            return;
        }
        LBInventoryManager.autoArmorCurrentSlot = hotbarIndex;
        SilentHotbar.selectSlotSilently(this, hotbarIndex, null, true, false, true);
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(stack));
        }
    }

    private static final int OP_OK = 0;

    private static final int OP_WAIT = 1;

    private static final int OP_STOP = 2;

    private int shouldOperate(boolean onlyHotbar) {
        if (!handleEvents()) {
            return OP_STOP;
        }
        if (!LBInventoryUtils.passedPostInventoryCloseDelay(postInventoryCloseDelay.getValue().intValue())) {
            return OP_STOP;
        }
        if (mc.playerController == null || !mc.playerController.getCurrentGameType().isSurvivalOrAdventure()) {
            return OP_STOP;
        }
        if (mc.thePlayer.openContainer == null
                || (mc.thePlayer.openContainer.windowId != 0 && (!onlyHotbar || notInContainers.isEnabled()))) {
            return OP_STOP;
        }
        if (onlyHotbar) {
            if (mc.currentScreen != null) {
                return OP_STOP;
            }
            return hotbar.isEnabled() ? OP_OK : OP_STOP;
        }
        if (invOpen.isEnabled() && !(mc.currentScreen instanceof GuiInventory)) {
            return OP_STOP;
        }
        if (LBInventoryManager.canClickInventory(true)) {
            return OP_OK;
        }
        LBInventoryManager.delay(50);
        return OP_WAIT;
    }

    private long sampleDelay() {
        double min = Math.max(0.0, delay.getMinValue());
        double max = Math.max(min, delay.getMaxValue());
        return (long) RandomUtils.nextDouble(min, max);
    }

    private int doClick(int slot, int button, int mode) {
        if (clickStage == 0) {
            int op = shouldOperate(false);
            if (op == OP_STOP) {
                LBInventoryManager.autoArmorLastSlot = -1;
                LBInventoryManager.autoArmorCurrentSlot = -1;
                clickStage = 0;
                return RES_ABORT;
            }
            if (op == OP_WAIT) {
                return RES_WAIT;
            }
            LBInventoryManager.autoArmorLastSlot = LBInventoryManager.autoArmorCurrentSlot;
            LBInventoryManager.autoArmorCurrentSlot = slot;
            if (simulateInventory.isEnabled() || invOpen.isEnabled()) {
                LBInventoryUtils.setServerOpenInventory(true);
            }
            if (LBInventoryUtils.isFirstInventoryClick) {
                LBInventoryUtils.isFirstInventoryClick = false;
                LBInventoryManager.delay(startDelay.getValue().longValue());
                clickStage = 1;
                return RES_WAIT;
            }
            clickStage = 1;
        }
        clickStage = 0;
        TickedActions.clickNextTick(this, slot, button, mode, false,
                mc.thePlayer.openContainer.windowId, null);
        LBInventoryManager.markScheduled();
        LBInventoryManager.delay(sampleDelay());
        return RES_ADVANCE;
    }

    public boolean equipFromHotbarStep() {
        switch (hotbarPhase) {
            case START: {
                int op = shouldOperate(true);
                if (op != OP_OK) {
                    abortHotbarFlow();
                    return true;
                }
                hotbarStacks.clear();
                for (int i = 0; i < mc.thePlayer.openContainer.inventorySlots.size(); i++) {
                    hotbarStacks.add(mc.thePlayer.openContainer.getSlot(i).getStack());
                }
                hotbarBestSet = ArmorComparator.getBestArmorSet(hotbarStacks, null);
                if (hotbarBestSet == null) {
                    abortHotbarFlow();
                    return true;
                }
                hotbarCursor = 0;
                hasClickedHotbar = false;
                hotbarPhase = HotbarPhase.SCAN;
                return false;
            }
            case SCAN: {
                if (!handleEvents() || mc.thePlayer.openContainer == null) {
                    abortHotbarFlow();
                    return true;
                }
                while (hotbarCursor <= 3) {
                    int armorType = hotbarCursor;
                    ArmorComparator.ArmorPiece piece = hotbarBestSet.get(armorType);
                    if (piece == null || piece.stack == null) {
                        hotbarCursor++;
                        continue;
                    }
                    Integer index = piece.index;
                    if (index == null) {
                        hotbarCursor++;
                        continue;
                    }
                    Integer hotbarIndex = LBInventoryUtils.toHotbarIndex(index, hotbarStacks.size());
                    if (hotbarIndex == null) {
                        hotbarCursor++;
                        continue;
                    }
                    if (TickedActions.isTicked(this, index) || TickedActions.isTicked(this, armorType + 5)) {
                        hotbarCursor++;
                        continue;
                    }
                    if (!StackExtensions.hasItemAgePassed(piece.stack, minItemAge.getValue().intValue())) {
                        hotbarCursor++;
                        continue;
                    }
                    int armorPos = EntityLiving.getArmorPosition(piece.stack) - 1;
                    if (armorPos < 0 || armorPos >= 4 || mc.thePlayer.inventory.armorInventory[armorPos] != null) {
                        hotbarCursor++;
                        continue;
                    }
                    hasClickedHotbar = true;
                    final ItemStack armorStack = piece.stack;
                    final int targetArmorPos = armorPos;
                    final int targetHotbarIndex = hotbarIndex;
                    TickedActions.nextTick(this, () -> {
                        LBInventoryManager.autoArmorCurrentSlot = targetHotbarIndex;
                        SilentHotbar.selectSlotSilently(AutoArmor.this, targetHotbarIndex, null, true, false, true);
                        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(armorStack));
                        }
                        if (mc.thePlayer != null) {
                            mc.thePlayer.inventory.armorInventory[targetArmorPos] = armorStack;
                            mc.thePlayer.inventory.mainInventory[targetHotbarIndex] = null;
                        }
                    });
                    hotbarCursor++;
                    if (delayedSlotSwitch.isEnabled()) {
                        LBInventoryManager.delay(sampleDelay());
                        return false;
                    }
                }
                hotbarPhase = HotbarPhase.FINAL_DELAY;
                return false;
            }
            case FINAL_DELAY: {
                LBInventoryManager.delay(sampleDelay());
                hotbarPhase = HotbarPhase.AWAIT;
                return false;
            }
            case AWAIT: {
                if (!TickedActions.isEmpty(this)) {
                    return false;
                }
                if (!hasClickedHotbar) {
                    abortHotbarFlow();
                    return true;
                }
                hasClickedHotbar = false;
                hotbarPhase = HotbarPhase.RESET;
                return false;
            }
            case RESET: {
                if (!TickedActions.isEmpty(this)) {
                    return false;
                }
                TickedActions.nextTick(this, () -> SilentHotbar.resetSlot(AutoArmor.this, false));
                hotbarPhase = HotbarPhase.START;
                hotbarCursor = 0;
                hotbarBestSet = null;
                return true;
            }
            default:
                abortHotbarFlow();
                return true;
        }
    }

    private void abortHotbarFlow() {
        hotbarPhase = HotbarPhase.START;
        hotbarCursor = 0;
        hasClickedHotbar = false;
        hotbarBestSet = null;
        LBInventoryManager.autoArmorCurrentSlot = -1;
        LBInventoryManager.autoArmorLastSlot = -1;
    }

    public boolean equipFromInventoryStep() {
        switch (invPhase) {
            case START: {
                int op = shouldOperate(false);
                if (op == OP_STOP) {
                    abortInvFlow();
                    return true;
                }
                if (op == OP_WAIT) {
                    return false;
                }
                invTypeCursor = 0;
                invPhase = InvPhase.SCAN_TYPE;
                return false;
            }
            case SCAN_TYPE: {
                while (true) {
                    int op = shouldOperate(false);
                    if (op == OP_STOP) {
                        abortInvFlow();
                        return true;
                    }
                    if (op == OP_WAIT) {
                        return false;
                    }
                    if (invTypeCursor > 3) {
                        invPhase = InvPhase.AWAIT;
                        return false;
                    }
                    int armorType = invTypeCursor;
                    refreshInvStacks();
                    ArmorComparator.ArmorSet armorSet = ArmorComparator.getBestArmorSet(invStacks, null);
                    if (armorSet == null) {
                        invTypeCursor++;
                        continue;
                    }
                    ArmorComparator.ArmorPiece piece = armorSet.get(armorType);
                    if (piece == null || piece.stack == null) {
                        invTypeCursor++;
                        continue;
                    }
                    Integer index = piece.index;
                    if (index == null) {
                        invTypeCursor++;
                        continue;
                    }
                    ItemStack stack = piece.stack;
                    if (TickedActions.isTicked(this, index) || TickedActions.isTicked(this, armorType + 5)) {
                        invTypeCursor++;
                        continue;
                    }
                    if (!StackExtensions.hasItemAgePassed(stack, minItemAge.getValue().intValue())) {
                        invTypeCursor++;
                        continue;
                    }
                    InventoryCleaner cleaner = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(InventoryCleaner.class);
                    if (cleaner != null && cleaner.canBeRepairedWithOther(stack, invStacks)) {
                        invTypeCursor++;
                        continue;
                    }
                    LBInventoryManager.autoArmorCurrentSlot = index;
                    ItemStack equipped = armorType + 5 < invStacks.size() ? invStacks.get(armorType + 5) : null;
                    invSeq = new ArrayList<>();
                    invSeqCursor = 0;
                    if (equipped == stack) {
                        LBInventoryManager.autoArmorCurrentSlot = -1;
                        LBInventoryManager.autoArmorLastSlot = -1;
                        invTypeCursor++;
                        continue;
                    } else if (equipped == null) {
                        invSeq.add(new int[]{index, 0, 1});
                    } else if (smartSwap.isEnabled()) {
                        invSeq.add(new int[]{index, 0, 0});
                        invSeq.add(new int[]{armorType + 5, 0, 0});
                        invSeq.add(new int[]{-999, 0, 0});
                    } else {
                        invSeq.add(new int[]{armorType + 5, 0, 4});
                        invSeq.add(new int[]{index, 0, 1});
                    }
                    invTypeCursor++;
                    invPhase = InvPhase.RUN_SEQ;
                    return false;
                }
            }
            case RUN_SEQ: {
                int r = runInvSequence();
                if (r == RES_ABORT) {
                    abortInvFlow();
                    return true;
                }
                if (r == RES_ADVANCE && invSeqCursor >= invSeq.size()) {
                    invPhase = InvPhase.SCAN_TYPE;
                }
                return false;
            }
            case AWAIT: {
                if (!TickedActions.isEmpty(this)) {
                    return false;
                }
                abortInvFlow();
                return true;
            }
            default:
                abortInvFlow();
                return true;
        }
    }

    private int runInvSequence() {
        while (invSeqCursor < invSeq.size()) {
            int[] click = invSeq.get(invSeqCursor);
            int r = doClick(click[0], click[1], click[2]);
            if (r == RES_ABORT) {
                return RES_ABORT;
            }
            if (r == RES_ADVANCE) {
                invSeqCursor++;
                continue;
            }
            return RES_WAIT;
        }
        return RES_ADVANCE;
    }

    private void refreshInvStacks() {
        invStacks.clear();
        for (int i = 0; i < mc.thePlayer.openContainer.inventorySlots.size(); i++) {
            invStacks.add(mc.thePlayer.openContainer.getSlot(i).getStack());
        }
    }

    private void abortInvFlow() {
        invPhase = InvPhase.START;
        invTypeCursor = 0;
        invSeqCursor = 0;
        invSeq = new ArrayList<>();
    }
}
