package dev.astra.module.impl.player;

import dev.astra.Main;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import dev.astra.util.RandomUtils;
import dev.astra.util.inventory.ArmorComparator;
import dev.astra.util.inventory.LBInventoryManager;
import dev.astra.util.inventory.LBInventoryUtils;
import dev.astra.util.inventory.StackExtensions;
import dev.astra.util.inventory.TickedActions;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBed;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class InventoryCleaner extends Module {
    public static InventoryCleaner INSTANCE;

    private final BooleanValue drop = new BooleanValue("Drop", true);

    private final BooleanValue sort = new BooleanValue("Sort", true);

    private final RangeSelectionNumberValue delay =
            new RangeSelectionNumberValue("Delay", 50.0, 50.0, 0.0, 1000.0, 1.0);

    private final NumberValue minItemAge = new NumberValue("MinItemAge", 0, 0, 2000, 1);

    private final BooleanValue limitStackCounts = new BooleanValue("LimitStackCounts", true);

    private final NumberValue maxBlockStacks = new NumberValue("MaxBlockStacks", 5, 0, 36, 1);

    private final NumberValue maxFoodStacks = new NumberValue("MaxFoodStacks", 5, 0, 36, 1);

    private final NumberValue maxThrowableStacks = new NumberValue("MaxThrowableStacks", 5, 0, 36, 1);

    private final NumberValue maxFishingRodStacks = new NumberValue("MaxFishingRodStacks", 1, 1, 10, 1);

    private final BooleanValue mergeStacksValue = new BooleanValue("MergeStacks", true);

    private final BooleanValue repairEquipment = new BooleanValue("RepairEquipment", true);

    public final BooleanValue invOpen = LBInventoryManager.invOpenValue;

    public final BooleanValue simulateInventory = LBInventoryManager.simulateInventoryValue;

    public final NumberValue postInventoryCloseDelay = LBInventoryManager.postInventoryCloseDelayValue;

    public final BooleanValue autoClose = LBInventoryManager.autoCloseValue;

    public final NumberValue startDelay = LBInventoryManager.startDelayValue;

    public final NumberValue closeDelay = LBInventoryManager.closeDelayValue;

    public final BooleanValue noMove = LBInventoryManager.noMoveValue;

    public final BooleanValue noMoveAir = LBInventoryManager.noMoveAirValue;

    public final BooleanValue noMoveGround = LBInventoryManager.noMoveGroundValue;

    private final BooleanValue randomSlot = new BooleanValue("RandomSlot", false);

    private final BooleanValue ignoreVehicles = new BooleanValue("IgnoreVehicles", false);

    private final BooleanValue onlyGoodPotions = new BooleanValue("OnlyGoodPotions", false);

    public final BooleanValue highlightSlot = LBInventoryManager.highlightSlotValue;

    public final ColorValue backgroundColor = LBInventoryManager.backgroundColorValue;

    public final NumberValue borderStrength = LBInventoryManager.borderStrengthValue;

    public final ColorValue borderColor = LBInventoryManager.borderColorValue;

    public final BooleanValue highlightUseful = new BooleanValue("HighlightUseful", true);

    private final ModeValue[] sortingValues = new ModeValue[9];

    private static final int RES_ADVANCE = 0;

    private static final int RES_WAIT = 1;

    private static final int RES_ABORT = -1;

    private static final int OP_OK = 0;

    private static final int OP_WAIT = 1;

    private static final int OP_STOP = 2;

    private int clickStage = 0;

    private enum RepairStage { COMPUTE, RUN_SEQ, READ_OUTPUT, AWAIT }

    private RepairStage repairStage = RepairStage.COMPUTE;

    private List<int[]> repairPairs = new ArrayList<>();

    private int repairPairCursor = 0;

    private List<int[]> repairSeq = new ArrayList<>();

    private int repairSeqCursor = 0;

    private boolean repairInFollowUp = false;

    private boolean mergeIterating = false;

    private List<Integer> mergeIndices = new ArrayList<>();

    private int mergeCursor = 0;

    private int mergeClickOfIndex = 0;

    private List<Integer> sortOrder = new ArrayList<>();

    private int sortOuterCursor = 0;

    private int sortHotbarIndex = -1;

    private Predicate<Item> sortTarget = null;

    private boolean sortStrictlyBest = true;

    private int sortStage = 0;

    private int sortScanCursor = 0;

    private List<Integer> dropOrder = new ArrayList<>();

    private int dropCursor = 0;

    private boolean conflictResolving = false;

    public InventoryCleaner() {
        super("InventoryCleaner", 0, Category.PLAYER);
        INSTANCE = this;
        for (int i = 0; i < 9; i++) {
            String name = "Slot" + (i + 1);
            String def = new String[]{"Sword", "Bow", "Pickaxe", "Axe", "Shovel", "Food", "Throwable", "FishingRod", "Block"}[i];
            sortingValues[i] = sortChoice(name, def);
        }
        addValues(drop, sort, delay, minItemAge,
                limitStackCounts, maxBlockStacks, maxFoodStacks, maxThrowableStacks, maxFishingRodStacks,
                mergeStacksValue, repairEquipment,
                invOpen, simulateInventory, postInventoryCloseDelay, autoClose, startDelay, closeDelay,
                noMove, noMoveAir, noMoveGround,
                randomSlot, ignoreVehicles, onlyGoodPotions,
                highlightSlot, highlightUseful, backgroundColor, borderStrength, borderColor,
                sortingValues[0], sortingValues[1], sortingValues[2], sortingValues[3], sortingValues[4],
                sortingValues[5], sortingValues[6], sortingValues[7], sortingValues[8]);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        TickedActions.clearTicked(this);
        resetFlows();
        LBInventoryManager.invCleanerCurrentSlot = -1;
        LBInventoryManager.invCleanerLastSlot = -1;
    }

    private void resetFlows() {
        repairStage = RepairStage.COMPUTE;
        repairPairs = new ArrayList<>();
        repairSeq = new ArrayList<>();
        repairPairCursor = 0;
        repairSeqCursor = 0;
        repairInFollowUp = false;
        mergeIterating = false;
        mergeIndices = new ArrayList<>();
        mergeCursor = 0;
        mergeClickOfIndex = 0;
        sortOrder = new ArrayList<>();
        sortOuterCursor = 0;
        sortStage = 0;
        sortScanCursor = 0;
        dropOrder = new ArrayList<>();
        dropCursor = 0;
        clickStage = 0;
    }

    public void refreshVisibility() {
        maxBlockStacks.setVisible(limitStackCounts.isEnabled());
        maxFoodStacks.setVisible(limitStackCounts.isEnabled());
        maxThrowableStacks.setVisible(limitStackCounts.isEnabled());
    }

    private ModeValue sortChoice(String name, String value) {
        ModeValue modeValue = new ModeValue(name, value, SORTING_KEYS);
        modeValue.setOnChange((oldValue, newValue) -> {
            if (conflictResolving || newValue.equals(oldValue)) {
                return newValue;
            }
            if (Arrays.asList(SINGLE_KEYS).contains(newValue)) {
                for (ModeValue another : sortingValues) {
                    if (another == null || another == modeValue) continue;
                    if (another.getValue().equals(newValue)) {
                        conflictResolving = true;
                        another.setValue(oldValue);
                        conflictResolving = false;
                        ChatUtil.sendMessage("\u00a78[\u00a79\u00a7lInventoryCleaner\u00a78] \u00a73Value \u00a7a"
                                + another.getName() + "\u00a73 was changed to \u00a7a" + oldValue
                                + "\u00a73 to prevent conflicts.");
                        break;
                    }
                }
            }
            return newValue;
        });
        return modeValue;
    }

    public boolean canBeSortedTo(int index, Item item) {
        return canBeSortedTo(index, item, null);
    }

    public boolean canBeSortedTo(int index, Item item, Integer stacksSize) {
        if (!isToggle() || !sort.isEnabled()) return false;
        int idx = index;
        if (stacksSize != null) {
            Integer hotbarIndex = LBInventoryUtils.toHotbarIndex(index, stacksSize);
            if (hotbarIndex == null) return false;
            idx = hotbarIndex;
        }
        if (idx < 0 || idx >= sortingValues.length) return false;
        Predicate<Item> target = SORTING_TARGETS.get(sortingValues[idx].getValue());
        return target != null && item != null && target.test(item);
    }

    private int shouldOperate() {
        if (!isToggle() || mc.thePlayer == null || mc.theWorld == null) {
            return OP_STOP;
        }
        if (!LBInventoryUtils.passedPostInventoryCloseDelay(postInventoryCloseDelay.getValue().intValue())) {
            return OP_STOP;
        }
        if (mc.playerController == null || !mc.playerController.getCurrentGameType().isSurvivalOrAdventure()) {
            return OP_STOP;
        }
        if (mc.thePlayer.openContainer == null || mc.thePlayer.openContainer.windowId != 0) {
            return OP_STOP;
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

    private long sampleDelay(long coerceTo) {
        double min = Math.max(0.0, delay.getMinValue());
        double max = Math.max(min, delay.getMaxValue());
        long d = (long) RandomUtils.nextDouble(min, max);
        return Math.min(d, coerceTo);
    }

    private int doClick(int slot, int button, int mode) {
        return doClick(slot, button, mode, false, Long.MAX_VALUE);
    }

    private int doClick(int slot, int button, int mode, boolean allowDuplicates, long coerceTo) {
        if (clickStage == 0) {
            int op = shouldOperate();
            if (op == OP_STOP) {
                LBInventoryManager.invCleanerLastSlot = -1;
                LBInventoryManager.invCleanerCurrentSlot = -1;
                clickStage = 0;
                return RES_ABORT;
            }
            if (op == OP_WAIT) {
                return RES_WAIT;
            }
            LBInventoryManager.invCleanerLastSlot = LBInventoryManager.invCleanerCurrentSlot;
            LBInventoryManager.invCleanerCurrentSlot = slot;
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
        TickedActions.clickNextTick(this, slot, button, mode, allowDuplicates,
                mc.thePlayer.openContainer.windowId, null);
        LBInventoryManager.markScheduled();
        LBInventoryManager.delay(sampleDelay(coerceTo));
        return RES_ADVANCE;
    }

    private void abortHighlightReset() {
        LBInventoryManager.invCleanerLastSlot = -1;
        LBInventoryManager.invCleanerCurrentSlot = -1;
    }

    public boolean repairStep() {
        while (true) {
            switch (repairStage) {
                case COMPUTE: {
                    int op = shouldOperate();
                    if (op == OP_STOP) {
                        resetFlows();
                        abortHighlightReset();
                        return true;
                    }
                    if (op == OP_WAIT) {
                        return false;
                    }
                    if (!repairEquipment.isEnabled()) {
                        resetFlows();
                        return true;
                    }
                    repairPairs = computePairsToRepair(mc.thePlayer.openContainer.getInventory());
                    repairPairCursor = 0;
                    if (repairPairs.isEmpty()) {
                        repairStage = RepairStage.AWAIT;
                        continue;
                    }
                    repairStage = RepairStage.RUN_SEQ;
                    continue;
                }
                case RUN_SEQ: {
                    int op = shouldOperate();
                    if (op == OP_STOP) {
                        resetFlows();
                        abortHighlightReset();
                        return true;
                    }
                    if (op == OP_WAIT) {
                        return false;
                    }
                    if (!repairInFollowUp && repairPairCursor >= repairPairs.size()) {
                        repairStage = RepairStage.AWAIT;
                        continue;
                    }
                    int[] pair = repairInFollowUp ? null : repairPairs.get(repairPairCursor);
                    int index1 = pair == null ? -1 : pair[0];
                    int index2 = pair == null ? -1 : pair[1];
                    if (!repairInFollowUp && repairSeq.isEmpty()
                            && (TickedActions.isTicked(this, index1) || TickedActions.isTicked(this, index2))) {
                        repairPairCursor++;
                        continue;
                    }
                    if (!repairInFollowUp && repairSeq.isEmpty()) {
                        repairSeq.add(new int[]{index1, 0, 0});
                        repairSeq.add(new int[]{1, 0, 0});
                        repairSeq.add(new int[]{index2, 0, 0});
                        repairSeq.add(new int[]{2, 0, 0});
                        repairSeqCursor = 0;
                    }
                    while (repairSeqCursor < repairSeq.size()) {
                        int[] click = repairSeq.get(repairSeqCursor);
                        int r = doClick(click[0], click[1], click[2]);
                        if (r == RES_ABORT) {
                            resetFlows();
                            return true;
                        }
                        if (r == RES_ADVANCE) {
                            repairSeqCursor++;
                            if (repairSeqCursor >= repairSeq.size()) {
                                if (repairInFollowUp) {
                                    repairInFollowUp = false;
                                    repairSeq = new ArrayList<>();
                                    repairSeqCursor = 0;
                                    repairPairCursor++;
                                } else {
                                    repairStage = RepairStage.READ_OUTPUT;
                                }
                            }
                            return false;
                        }
                        return false;
                    }
                    continue;
                }
                case READ_OUTPUT: {
                    int r = readRepairOutput();
                    if (r == RES_ABORT) {
                        resetFlows();
                        return true;
                    }
                    if (r == RES_ADVANCE) {
                        repairStage = RepairStage.RUN_SEQ;
                        continue;
                    }
                    return false;
                }
                case AWAIT: {
                    if (!TickedActions.isEmpty(this)) {
                        return false;
                    }
                    resetFlows();
                    return true;
                }
                default:
                    resetFlows();
                    return true;
            }
        }
    }

    private static class IndexedStack {
        final int index;
        final ItemStack stack;
        IndexedStack(int index, ItemStack stack) {
            this.index = index;
            this.stack = stack;
        }
    }

    private List<int[]> computePairsToRepair(List<ItemStack> stacks) {
        List<IndexedStack> candidates = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (StackExtensions.isEmpty(stack)) continue;
            if (!StackExtensions.hasItemAgePassed(stack, minItemAge.getValue().intValue())) continue;
            if (shouldBeRepaired(stack)) {
                candidates.add(new IndexedStack(i, stack));
            }
        }
        List<List<IndexedStack>> groups = new ArrayList<>();
        for (IndexedStack candidate : candidates) {
            boolean grouped = false;
            for (List<IndexedStack> group : groups) {
                if (group.get(0).stack.getItem() == candidate.stack.getItem()) {
                    group.add(candidate);
                    grouped = true;
                    break;
                }
            }
            if (!grouped) {
                List<IndexedStack> group = new ArrayList<>();
                group.add(candidate);
                groups.add(group);
            }
        }
        List<int[]> pairs = new ArrayList<>();
        for (List<IndexedStack> group : groups) {
            boolean anyUsefulDamaged = false;
            int maxTotalDurability = Integer.MIN_VALUE;
            for (IndexedStack indexed : group) {
                if (isStackUseful(indexed.stack, stacks, null, true, false) && indexed.stack.isItemDamaged()) {
                    anyUsefulDamaged = true;
                }
                maxTotalDurability = Math.max(maxTotalDurability, StackExtensions.totalDurability(indexed.stack));
            }
            if (!anyUsefulDamaged) continue;
            RepairTriple best = null;
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    Integer durability = getCombinedDurabilityIfBeneficial(group.get(i).stack, group.get(j).stack);
                    if (durability != null && (best == null || durability > best.durability)) {
                        best = new RepairTriple(group.get(i).index, group.get(j).index, durability);
                    }
                }
            }
            if (best != null && best.durability >= maxTotalDurability) {
                pairs.add(new int[]{best.index1, best.index2});
            }
        }
        return pairs;
    }

    private static class RepairTriple {
        final int index1;
        final int index2;
        final int durability;
        RepairTriple(int index1, int index2, int durability) {
            this.index1 = index1;
            this.index2 = index2;
            this.durability = durability;
        }
    }

    private Integer getCombinedDurabilityIfBeneficial(ItemStack stack1, ItemStack stack2) {
        int combinedDurability = Math.min(
                StackExtensions.durability(stack1) + StackExtensions.durability(stack2) + stack1.getMaxDamage() * 5 / 100,
                stack1.getMaxDamage());
        if (StackExtensions.totalDurability(stack1) >= combinedDurability
                || StackExtensions.totalDurability(stack2) >= combinedDurability) {
            return null;
        }
        return combinedDurability;
    }

    private boolean shouldBeRepaired(ItemStack stack) {
        if (StackExtensions.isEmpty(stack)) return false;
        if (!StackExtensions.isRepairable(stack)) return false;
        if (!stack.isItemEnchanted()) return true;
        return StackExtensions.enchantmentCount(stack) == 1
                && StackExtensions.enchantments(stack).containsKey(Enchantment.unbreaking);
    }

    public boolean canBeRepairedWithOther(ItemStack stack, List<ItemStack> stacks) {
        if (!isToggle() || mc.thePlayer == null || mc.theWorld == null || !repairEquipment.isEnabled()) {
            return false;
        }
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (!shouldBeRepaired(stack)) return false;
        for (ItemStack otherStack : stacks) {
            if (StackExtensions.isEmpty(otherStack) || otherStack == stack) continue;
            if (otherStack.getItem() != item) continue;
            if (getCombinedDurabilityIfBeneficial(stack, otherStack) != null) {
                return true;
            }
        }
        return false;
    }

    private int readRepairOutput() {
        ItemStack repairedStack = mc.thePlayer.openContainer.getSlot(0).getStack();
        repairSeq = new ArrayList<>();
        repairSeqCursor = 0;
        if (StackExtensions.isEmpty(repairedStack)) {
            repairPairCursor++;
            return RES_ADVANCE;
        }
        Item repairedItem = repairedStack.getItem();
        boolean consumed = false;
        if (repairedItem instanceof net.minecraft.item.ItemArmor) {
            net.minecraft.item.ItemArmor armorItem = (net.minecraft.item.ItemArmor) repairedItem;
            int armorSlot = armorItem.armorType + 5;
            boolean equipAfterCrafting = true;
            AutoArmor autoArmor = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(AutoArmor.class);
            boolean autoArmorActive = autoArmor != null && autoArmor.handleEventsPublic();
            if (mc.thePlayer.openContainer.getSlot(armorSlot).getHasStack()) {
                if (autoArmorActive && autoArmor.smartSwap.isEnabled()) {
                    repairSeq.add(new int[]{0, 0, 0});
                    repairSeq.add(new int[]{armorSlot, 0, 0});
                    repairSeq.add(new int[]{-999, 0, 0});
                    consumed = true;
                } else if (drop.isEnabled() || autoArmorActive) {
                    repairSeq.add(new int[]{armorSlot, 0, 4});
                } else {
                    equipAfterCrafting = false;
                }
            }
            if (!consumed && equipAfterCrafting) {
                repairSeq.add(new int[]{0, 0, 0});
                repairSeq.add(new int[]{armorItem.armorType + 5, 0, 0});
                consumed = true;
            }
        }
        if (!consumed && sort.isEnabled()) {
            List<ItemStack> stacks = mc.thePlayer.openContainer.getInventory();
            for (int hotbarIndex = 0; hotbarIndex <= 8; hotbarIndex++) {
                if (!canBeSortedTo(hotbarIndex, repairedItem)) {
                    continue;
                }
                ItemStack hotbarStack = stacks.size() - 9 + hotbarIndex < stacks.size()
                        ? stacks.get(stacks.size() - 9 + hotbarIndex) : null;
                if (!canBeSortedTo(hotbarIndex, StackExtensions.isEmpty(hotbarStack) ? null : hotbarStack.getItem())
                        || !isStackUseful(hotbarStack, stacks, null, false, true)) {
                    repairSeq.add(new int[]{0, hotbarIndex, 2});
                    consumed = true;
                    break;
                }
            }
        }
        if (!consumed) {
            repairSeq.add(new int[]{0, 0, 1});
        }
        if (repairSeq.isEmpty()) {
            repairPairCursor++;
        } else {
            repairInFollowUp = true;
        }
        return RES_ADVANCE;
    }

    public boolean mergeStep() {
        if (!mergeIterating) {
            int op = shouldOperate();
            if (op == OP_STOP) {
                resetFlows();
                abortHighlightReset();
                return true;
            }
            if (op == OP_WAIT) {
                return false;
            }
            if (!mergeStacksValue.isEnabled()) {
                resetFlows();
                return true;
            }
            List<ItemStack> stacks = mc.thePlayer.openContainer.getInventory();
            mergeIndices = computeIndicesToDoubleClick(stacks);
            mergeCursor = 0;
            mergeClickOfIndex = 0;
            if (mergeIndices.isEmpty()) {
                resetFlows();
                return true;
            }
            mergeIterating = true;
        }
        boolean suspended = false;
        while (mergeCursor < mergeIndices.size()) {
            int op = shouldOperate();
            if (op == OP_STOP) {
                resetFlows();
                abortHighlightReset();
                return true;
            }
            if (op == OP_WAIT) {
                suspended = true;
                break;
            }
            int index = mergeIndices.get(mergeCursor);
            if (TickedActions.isTicked(this, index)) {
                mergeCursor++;
                mergeClickOfIndex = 0;
                continue;
            }
            int r = doClick(index, 0, 0, true, 100);
            if (r == RES_ABORT) {
                resetFlows();
                return true;
            }
            if (r == RES_ADVANCE) {
                mergeClickOfIndex++;
                if (mergeClickOfIndex >= 3) {
                    mergeCursor++;
                    mergeClickOfIndex = 0;
                }
            }
            suspended = true;
            break;
        }
        if (suspended) {
            return false;
        }
        if (!TickedActions.isEmpty(this)) {
            return false;
        }
        mergeIterating = false;
        return true;
    }

    private List<Integer> computeIndicesToDoubleClick(List<ItemStack> stacks) {
        Map<Item, List<Integer>> byItem = new LinkedHashMap<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null) continue;
            if (!StackExtensions.hasItemAgePassed(stack, minItemAge.getValue().intValue())) continue;
            if (stack.stackSize == stack.getMaxStackSize()) continue;
            if (!isStackUseful(stack, stacks, null, true, false)) continue;
            byItem.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(i);
        }
        List<Integer> indicesToDoubleClick = new ArrayList<>();
        for (Map.Entry<Item, List<Integer>> entry : byItem.entrySet()) {
            List<Integer> indices = entry.getValue();
            indices.sort((a, b) -> Integer.compare(b, a));
            indices.sort((a, b) -> {
                boolean aSortable = canBeSortedTo(a, stacks.get(a).getItem(), stacks.size());
                boolean bSortable = canBeSortedTo(b, stacks.get(b).getItem(), stacks.size());
                return Boolean.compare(bSortable, aSortable);
            });
            outer:
            for (int clickedIndex : indices) {
                ItemStack clickedStack = stacks.get(clickedIndex);
                for (int mergeIndex : indices) {
                    if (mergeIndex == clickedIndex) continue;
                    ItemStack stackToMerge = stacks.get(mergeIndex);
                    if (clickedStack.stackSize + stackToMerge.stackSize <= clickedStack.getMaxStackSize()
                            && clickedStack.isItemEqual(stackToMerge)
                            && ItemStack.areItemStackTagsEqual(clickedStack, stackToMerge)) {
                        indicesToDoubleClick.add(clickedIndex);
                        break outer;
                    }
                }
            }
        }
        return indicesToDoubleClick;
    }

    public boolean sortStep() {
        while (true) {
            int op = shouldOperate();
            if (op == OP_STOP) {
                resetFlows();
                abortHighlightReset();
                return true;
            }
            if (op == OP_WAIT) {
                return false;
            }
            if (!sort.isEnabled()) {
                resetFlows();
                return true;
            }
            if (sortOrder.size() != 9) {
                sortOrder = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
                if (randomSlot.isEnabled()) {
                    Collections.shuffle(sortOrder);
                }
                sortOuterCursor = 0;
                sortStage = 0;
            }
            boolean suspended = false;
            while (sortOuterCursor < sortOrder.size()) {
                op = shouldOperate();
                if (op == OP_STOP) {
                    resetFlows();
                    abortHighlightReset();
                    return true;
                }
                if (op == OP_WAIT) {
                    suspended = true;
                    break;
                }
                int hotbarIndex = sortOrder.get(sortOuterCursor);
                Predicate<Item> target = SORTING_TARGETS.get(sortingValues[hotbarIndex].getValue());
                if (target == null) {
                    sortOuterCursor++;
                    sortStage = 0;
                    continue;
                }
                List<ItemStack> stacks = mc.thePlayer.openContainer.getInventory();
                if (sortStage == 0) {
                    sortHotbarIndex = hotbarIndex;
                    sortTarget = target;
                    sortStrictlyBest = true;
                    sortStage = 1;
                    sortScanCursor = 0;
                }
                boolean slotFinished = false;
                while (!slotFinished) {
                    int index = sortHotbarIndex + 36;
                    ItemStack stack = index < stacks.size() ? stacks.get(index) : null;
                    Item item = StackExtensions.isEmpty(stack) ? null : stack.getItem();
                    if (sortStage == 1) {
                        if (item != null && sortTarget.test(item)
                                && isStackUseful(stack, stacks, null, false, sortStrictlyBest)) {
                            sortStage = 4;
                            continue;
                        }
                        sortStage = 2;
                        sortScanCursor = 0;
                        continue;
                    }
                    if (sortStage == 2) {
                        boolean found = false;
                        while (sortScanCursor < stacks.size()) {
                            int otherIndex = sortScanCursor++;
                            if (TickedActions.isTicked(this, otherIndex)) {
                                continue;
                            }
                            ItemStack otherStack = stacks.get(otherIndex);
                            Item otherItem = StackExtensions.isEmpty(otherStack) ? null : otherStack.getItem();
                            if (otherItem == null || !sortTarget.test(otherItem)
                                    || !isStackUseful(otherStack, stacks, null, false, sortStrictlyBest)
                                    || canBeSortedTo(otherIndex, otherItem, stacks.size())) {
                                continue;
                            }
                            found = true;
                            if (StackExtensions.hasItemAgePassed(otherStack, minItemAge.getValue().intValue())) {
                                int r = doClick(otherIndex, sortHotbarIndex, 2);
                                if (r == RES_ABORT) {
                                    resetFlows();
                                    return true;
                                }
                                sortStage = 4;
                                suspended = true;
                                break;
                            }
                            sortStage = 4;
                            break;
                        }
                        if (suspended) break;
                        if (!found) {
                            if (sortStrictlyBest) {
                                sortStrictlyBest = false;
                                sortStage = 1;
                            } else {
                                sortStage = 4;
                            }
                        }
                        continue;
                    }
                    if (sortStage == 4) {
                        sortStage = 0;
                        sortOuterCursor++;
                        slotFinished = true;
                    }
                }
                if (suspended) {
                    break;
                }
            }
            if (suspended) {
                return false;
            }
            if (!TickedActions.isEmpty(this)) {
                return false;
            }
            sortOrder = new ArrayList<>();
            resetFlowsSoft();
            return true;
        }
    }

    private void resetFlowsSoft() {
        sortOrder = new ArrayList<>();
        sortOuterCursor = 0;
        sortStage = 0;
        sortScanCursor = 0;
        clickStage = 0;
    }

    public boolean dropStep() {
        while (true) {
            int op = shouldOperate();
            if (op == OP_STOP) {
                resetFlows();
                abortHighlightReset();
                return true;
            }
            if (op == OP_WAIT) {
                return false;
            }
            if (!drop.isEnabled()) {
                resetFlows();
                return true;
            }
            if (dropOrder.isEmpty()) {
                dropOrder = new ArrayList<>();
                for (int i = 0; i < mc.thePlayer.openContainer.inventorySlots.size(); i++) {
                    dropOrder.add(i);
                }
                if (randomSlot.isEnabled()) {
                    Collections.shuffle(dropOrder);
                }
                dropCursor = 0;
            }
            boolean suspended = false;
            while (dropCursor < dropOrder.size()) {
                op = shouldOperate();
                if (op == OP_STOP) {
                    resetFlows();
                    abortHighlightReset();
                    return true;
                }
                if (op == OP_WAIT) {
                    suspended = true;
                    break;
                }
                int index = dropOrder.get(dropCursor);
                if (TickedActions.isTicked(this, index)) {
                    dropCursor++;
                    continue;
                }
                List<ItemStack> stacks = mc.thePlayer.openContainer.getInventory();
                ItemStack stack = index < stacks.size() ? stacks.get(index) : null;
                if (StackExtensions.isEmpty(stack)) {
                    dropCursor++;
                    continue;
                }
                if (!StackExtensions.hasItemAgePassed(stack, minItemAge.getValue().intValue())) {
                    dropCursor++;
                    continue;
                }
                if (!isStackUseful(stack, stacks)) {
                    int r = doClick(index, 1, 4);
                    if (r == RES_ABORT) {
                        resetFlows();
                        return true;
                    }
                    if (r == RES_ADVANCE) {
                        dropCursor++;
                    }
                    suspended = true;
                    break;
                }
                dropCursor++;
            }
            if (suspended) {
                return false;
            }
            if (!TickedActions.isEmpty(this)) {
                return false;
            }
            dropOrder = new ArrayList<>();
            dropCursor = 0;
            clickStage = 0;
            return true;
        }
    }

    public boolean isStackUseful(ItemStack stack, List<ItemStack> stacks) {
        return isStackUseful(stack, stacks, null, false, false);
    }

    public boolean isStackUseful(ItemStack stack, List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap,
                                 boolean noLimits, boolean strictlyBest) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (inItemsWhitelist(item)) return true;
        if (item instanceof ItemEnderPearl || item instanceof ItemEnchantedBook || item instanceof ItemBed) return true;
        if (item instanceof ItemFood) return isUsefulFood(stack, stacks, entityStacksMap, noLimits, strictlyBest);
        if (item instanceof ItemBlock) return isUsefulBlock(stack, stacks, entityStacksMap, noLimits, strictlyBest);
        if (item instanceof net.minecraft.item.ItemArmor || item instanceof ItemTool || item instanceof ItemSword
                || item instanceof ItemBow || item instanceof ItemFishingRod || item instanceof ItemShears) {
            return isUsefulEquipment(stack, stacks, entityStacksMap);
        }
        if (item instanceof ItemBoat || item instanceof ItemMinecart) return !ignoreVehicles.isEnabled();
        if (item instanceof ItemPotion) return isUsefulPotion(stack);
        if (item instanceof ItemBucket) return isUsefulBucket(stack, stacks, entityStacksMap);
        if (item instanceof ItemFlintAndSteel) return isUsefulLighter(stack, stacks, entityStacksMap);
        if (inThrowableItems(item)) return isUsefulThrowable(stack, stacks, entityStacksMap, noLimits, strictlyBest);
        return false;
    }

    private boolean isUsefulEquipment(ItemStack stack, List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap) {
        Item item = stack.getItem();
        if (item instanceof net.minecraft.item.ItemArmor) {
            ArmorComparator.ArmorSet bestSet = ArmorComparator.getBestArmorSet(stacks, entityStacksMap);
            return bestSet == null || bestSet.containsStack(stack);
        }
        if (item instanceof ItemTool) {
            Block blockType;
            if (item instanceof ItemAxe) blockType = Blocks.log;
            else if (item instanceof ItemPickaxe) blockType = Blocks.stone;
            else blockType = Blocks.dirt;
            return hasBestParameters(stack, stacks, entityStacksMap, s ->
                    s.getItem().getStrVsBlock(s, blockType) * StackExtensions.durability(s));
        }
        if (item instanceof ItemFishingRod) {
            int fishingRod = 0;
            for (ItemStack other : stacks) {
                if (!StackExtensions.isEmpty(other) && other.getItem() instanceof ItemFishingRod) fishingRod++;
            }
            if (fishingRod <= maxFishingRodStacks.getValue().intValue()) return true;
            return hasBestParameters(stack, stacks, entityStacksMap,
                    s -> (float) StackExtensions.durability(s));
        }
        if (item instanceof ItemShears) {
            return hasBestParameters(stack, stacks, entityStacksMap, s ->
                    (float) StackExtensions.durability(s) * StackExtensions.getEnchantmentLevel(s, Enchantment.efficiency));
        }
        if (item instanceof ItemSword) {
            return hasBestParameters(stack, stacks, entityStacksMap, StackExtensions::attackDamage);
        }
        if (item instanceof ItemBow) {
            return hasBestParameters(stack, stacks, entityStacksMap, s ->
                    (float) StackExtensions.getEnchantmentLevel(s, Enchantment.power));
        }
        return false;
    }

    private boolean isUsefulPotion(ItemStack stack) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (!(item instanceof ItemPotion)) return false;
        ItemPotion potionItem = (ItemPotion) item;
        boolean isSplash = StackExtensions.isSplashPotion(stack);
        List<PotionEffect> effects = potionItem.getEffects(stack);
        if (effects == null) return false;
        boolean isHarmful = false;
        for (PotionEffect effect : effects) {
            if (inNegativeEffectIds(effect.getPotionID())) {
                isHarmful = true;
                break;
            }
        }
        return !isHarmful || (!onlyGoodPotions.isEnabled() && isSplash);
    }

    private boolean isUsefulLighter(ItemStack stack, List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (!(item instanceof ItemFlintAndSteel)) return false;
        int index = identityIndexOf(stacks, stack);
        if (canBeSortedTo(index, item, stacks.size())) return true;
        List<ItemStack> stacksToIterate = new ArrayList<>(stacks);
        double distanceSqToItem = 0.0D;
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            EntityItem own = entityStacksMap.get(stack);
            if (own == null) return false;
            distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(own);
            stacksToIterate.addAll(entityStacksMap.keySet());
        }
        for (int otherIndex = 0; otherIndex < stacksToIterate.size(); otherIndex++) {
            ItemStack otherStack = stacksToIterate.get(otherIndex);
            if (otherStack == stack) continue;
            Item otherItem = StackExtensions.isEmpty(otherStack) ? null : otherStack.getItem();
            if (!(otherItem instanceof ItemFlintAndSteel)) continue;
            int adjustedIndex = otherIndex > stacks.size() - 1 ? -1 : otherIndex;
            if (index == adjustedIndex) {
                EntityItem otherEntityItem = entityStacksMap == null ? null : entityStacksMap.get(otherStack);
                if (otherEntityItem == null) continue;
                if (distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)) {
                    return false;
                }
                continue;
            }
            if (canBeSortedTo(adjustedIndex, otherItem, stacks.size())
                    || StackExtensions.totalDurability(otherStack) > StackExtensions.totalDurability(stack)
                    || adjustedIndex > index) {
                return false;
            }
        }
        return true;
    }

    private boolean isUsefulFood(ItemStack stack, List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap,
                                 boolean ignoreLimits, boolean strictlyBest) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (!(item instanceof ItemFood)) return false;
        if (ignoreLimits || !limitStackCounts.isEnabled()) {
            if (!strictlyBest) return true;
        } else if (maxFoodStacks.getValue().intValue() == 0) {
            return false;
        }
        ItemFood foodItem = (ItemFood) item;
        float stackSaturation = foodItem.getSaturationModifier(stack) * stack.stackSize;
        int index = identityIndexOf(stacks, stack);
        boolean isSorted = canBeSortedTo(index, item, stacks.size());
        List<ItemStack> stacksToIterate = new ArrayList<>(stacks);
        double distanceSqToItem = 0.0D;
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            EntityItem own = entityStacksMap.get(stack);
            if (own == null) return false;
            distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(own);
            stacksToIterate.addAll(entityStacksMap.keySet());
        }
        int betterCount = 0;
        for (int otherIndex = 0; otherIndex < stacksToIterate.size(); otherIndex++) {
            ItemStack otherStack = stacksToIterate.get(otherIndex);
            if (otherStack == stack) continue;
            Item otherItem = StackExtensions.isEmpty(otherStack) ? null : otherStack.getItem();
            if (!(otherItem instanceof ItemFood)) continue;
            int adjustedIndex = otherIndex > stacks.size() - 1 ? -1 : otherIndex;
            float otherStackSaturation = ((ItemFood) otherItem).getSaturationModifier(otherStack) * otherStack.stackSize;
            int cmp = Float.compare(otherStackSaturation, stackSaturation);
            if (cmp > 0) {
                betterCount++;
                continue;
            }
            if (cmp != 0) continue;
            if (index == adjustedIndex) {
                EntityItem otherEntityItem = entityStacksMap == null ? null : entityStacksMap.get(otherStack);
                if (otherEntityItem == null) continue;
                if (distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)) {
                    betterCount++;
                }
            } else {
                boolean isOtherSorted = canBeSortedTo(adjustedIndex, otherItem, stacks.size());
                if (!isSorted && (isOtherSorted || adjustedIndex > index)) {
                    betterCount++;
                }
            }
        }
        return strictlyBest ? betterCount == 0 : betterCount < maxFoodStacks.getValue().intValue();
    }

    private boolean isUsefulBlock(ItemStack stack, List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap,
                                  boolean ignoreLimits, boolean strictlyBest) {
        if (!StackExtensions.isSuitableBlock(stack)) return false;
        if (ignoreLimits || !limitStackCounts.isEnabled()) {
            if (!strictlyBest) return true;
        } else if (maxBlockStacks.getValue().intValue() == 0) {
            return false;
        }
        int index = identityIndexOf(stacks, stack);
        boolean isSorted = canBeSortedTo(index, stack.getItem(), stacks.size());
        List<ItemStack> stacksToIterate = new ArrayList<>(stacks);
        double distanceSqToItem = 0.0D;
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            EntityItem own = entityStacksMap.get(stack);
            if (own == null) return false;
            distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(own);
            stacksToIterate.addAll(entityStacksMap.keySet());
        }
        int betterCount = 0;
        for (int otherIndex = 0; otherIndex < stacksToIterate.size(); otherIndex++) {
            ItemStack otherStack = stacksToIterate.get(otherIndex);
            if (otherStack == stack || !StackExtensions.isSuitableBlock(otherStack)) continue;
            int adjustedIndex = otherIndex > stacks.size() - 1 ? -1 : otherIndex;
            int cmp = Integer.compare(otherStack.stackSize, stack.stackSize);
            if (cmp > 0) {
                betterCount++;
                continue;
            }
            if (cmp != 0) continue;
            if (index == adjustedIndex) {
                EntityItem otherEntityItem = entityStacksMap == null ? null : entityStacksMap.get(otherStack);
                if (otherEntityItem == null) continue;
                if (distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)) {
                    betterCount++;
                }
            } else {
                boolean isOtherSorted = canBeSortedTo(adjustedIndex, otherStack.getItem(), stacks.size());
                if (!isSorted && (isOtherSorted || adjustedIndex > index)) {
                    betterCount++;
                }
            }
        }
        return strictlyBest ? betterCount == 0 : betterCount < maxBlockStacks.getValue().intValue();
    }

    private boolean isUsefulThrowable(ItemStack stack, List<ItemStack> stacks,
                                      Map<ItemStack, EntityItem> entityStacksMap,
                                      boolean ignoreLimits, boolean strictlyBest) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (!inThrowableItems(item)) return false;
        if (ignoreLimits || !limitStackCounts.isEnabled()) {
            if (!strictlyBest) return true;
        } else if (maxBlockStacks.getValue().intValue() == 0) {
            return false;
        }
        int index = identityIndexOf(stacks, stack);
        boolean isSorted = canBeSortedTo(index, item, stacks.size());
        List<ItemStack> stacksToIterate = new ArrayList<>(stacks);
        double distanceSqToItem = 0.0D;
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            EntityItem own = entityStacksMap.get(stack);
            if (own == null) return false;
            distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(own);
            stacksToIterate.addAll(entityStacksMap.keySet());
        }
        int betterCount = 0;
        for (int otherIndex = 0; otherIndex < stacksToIterate.size(); otherIndex++) {
            ItemStack otherStack = stacksToIterate.get(otherIndex);
            if (otherStack == stack) continue;
            Item otherItem = StackExtensions.isEmpty(otherStack) ? null : otherStack.getItem();
            if (!inThrowableItems(otherItem)) continue;
            int adjustedIndex = otherIndex > stacks.size() - 1 ? -1 : otherIndex;
            int cmp = Integer.compare(otherStack.stackSize, stack.stackSize);
            if (cmp > 0) {
                betterCount++;
                continue;
            }
            if (cmp != 0) continue;
            if (index == adjustedIndex) {
                EntityItem otherEntityItem = entityStacksMap == null ? null : entityStacksMap.get(otherStack);
                if (otherEntityItem == null) continue;
                if (distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)) {
                    betterCount++;
                }
            } else {
                boolean isOtherSorted = canBeSortedTo(adjustedIndex, otherItem, stacks.size());
                if (!isSorted && (isOtherSorted || adjustedIndex > index)) {
                    betterCount++;
                }
            }
        }
        return strictlyBest ? betterCount == 0 : betterCount < maxThrowableStacks.getValue().intValue();
    }

    private boolean isUsefulBucket(ItemStack stack, List<ItemStack> stacks, Map<ItemStack, EntityItem> entityStacksMap) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        if (!(item instanceof ItemBucket)) return false;
        int index = identityIndexOf(stacks, stack);
        if (canBeSortedTo(index, item, stacks.size())) return true;
        List<ItemStack> stacksToIterate = new ArrayList<>(stacks);
        double distanceSqToItem = 0.0D;
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            EntityItem own = entityStacksMap.get(stack);
            if (own == null) return false;
            distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(own);
            stacksToIterate.addAll(entityStacksMap.keySet());
        }
        for (int otherIndex = 0; otherIndex < stacksToIterate.size(); otherIndex++) {
            ItemStack otherStack = stacksToIterate.get(otherIndex);
            if (otherStack == stack) continue;
            Item otherItem = StackExtensions.isEmpty(otherStack) ? null : otherStack.getItem();
            if (otherItem != item) continue;
            int adjustedIndex = otherIndex > stacks.size() - 1 ? -1 : otherIndex;
            if (index == adjustedIndex) {
                EntityItem otherEntityItem = entityStacksMap == null ? null : entityStacksMap.get(otherStack);
                if (otherEntityItem == null) continue;
                if (distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)) {
                    return false;
                }
                continue;
            }
            if (canBeSortedTo(adjustedIndex, otherItem, stacks.size()) || adjustedIndex > index) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBestParameters(ItemStack stack, List<ItemStack> stacks,
                                      Map<ItemStack, EntityItem> entityStacksMap,
                                      StackParam parameters) {
        if (StackExtensions.isEmpty(stack)) return false;
        Item item = stack.getItem();
        int index = identityIndexOf(stacks, stack);
        float currentStats = parameters.apply(stack);
        boolean isSorted = canBeSortedTo(index, item, stacks.size());
        List<ItemStack> stacksToIterate = new ArrayList<>(stacks);
        double distanceSqToItem = 0.0D;
        if (entityStacksMap != null && !entityStacksMap.isEmpty()) {
            EntityItem own = entityStacksMap.get(stack);
            if (own == null) return false;
            distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(own);
            stacksToIterate.addAll(entityStacksMap.keySet());
        }
        for (int otherIndex = 0; otherIndex < stacksToIterate.size(); otherIndex++) {
            ItemStack otherStack = stacksToIterate.get(otherIndex);
            Item otherItem = StackExtensions.isEmpty(otherStack) ? null : otherStack.getItem();
            if (otherItem == null) continue;
            if (stack == otherStack || item.getClass() != otherItem.getClass()) continue;
            int adjustedIndex = otherIndex > stacks.size() - 1 ? -1 : otherIndex;
            float otherStats = parameters.apply(otherStack);
            boolean isOtherSorted = canBeSortedTo(adjustedIndex, otherItem, stacks.size());
            int cmp = Float.compare(otherStats, currentStats);
            if (cmp > 0) {
                return false;
            }
            if (cmp < 0) continue;
            int enchantmentCmp = Integer.compare(StackExtensions.enchantmentSum(otherStack), StackExtensions.enchantmentSum(stack));
            if (enchantmentCmp > 0) {
                return false;
            }
            if (enchantmentCmp < 0) continue;
            int durabilityCmp = Integer.compare(StackExtensions.totalDurability(otherStack), StackExtensions.totalDurability(stack));
            if (durabilityCmp > 0) {
                return false;
            }
            if (durabilityCmp < 0) continue;
            if (index == adjustedIndex) {
                EntityItem otherEntityItem = entityStacksMap == null ? null : entityStacksMap.get(otherStack);
                if (otherEntityItem == null) continue;
                int distCmp = Double.compare(distanceSqToItem, mc.thePlayer.getDistanceSqToEntity(otherEntityItem));
                if (distCmp > 0) {
                    return false;
                }
                if (distCmp == 0) {
                    return true;
                }
            } else if (!isSorted && (isOtherSorted || adjustedIndex > index)) {
                return false;
            }
        }
        return true;
    }

    private interface StackParam {
        float apply(ItemStack stack);
    }

    private static boolean inItemsWhitelist(Item item) {
        return item == Items.arrow || item == Items.diamond || item == Items.iron_ingot
                || item == Items.gold_ingot || item == Items.stick;
    }

    private static boolean inThrowableItems(Item item) {
        return item == Items.egg || item == Items.snowball;
    }

    public static boolean inNegativeEffectIds(int id) {
        return id == Potion.moveSlowdown.id || id == Potion.digSlowdown.id || id == Potion.harm.id
                || id == Potion.confusion.id || id == Potion.blindness.id || id == Potion.hunger.id
                || id == Potion.weakness.id || id == Potion.poison.id || id == Potion.wither.id;
    }

    public static final String[] SINGLE_KEYS = {"Sword", "Bow", "Pickaxe", "Axe", "Shovel"};

    private static final Map<String, Predicate<Item>> SORTING_TARGETS = buildSortingTargets();

    public static final String[] SORTING_KEYS = SORTING_TARGETS.keySet().toArray(new String[0]);

    private static Map<String, Predicate<Item>> buildSortingTargets() {
        Map<String, Predicate<Item>> targets = new LinkedHashMap<>();
        targets.put("Sword", item -> item instanceof ItemSword);
        targets.put("Bow", item -> item instanceof ItemBow);
        targets.put("Pickaxe", item -> item instanceof ItemPickaxe);
        targets.put("Axe", item -> item instanceof ItemAxe);
        targets.put("Shovel", item -> item instanceof ItemSpade);
        targets.put("Food", item -> item instanceof ItemFood);
        targets.put("Block", item -> item instanceof ItemBlock);
        targets.put("Water", item -> item == Items.water_bucket || item == Items.bucket);
        targets.put("Fire", item -> item instanceof ItemFlintAndSteel || item == Items.lava_bucket || item == Items.bucket);
        targets.put("Gapple", item -> item instanceof ItemAppleGold);
        targets.put("Pearl", item -> item instanceof ItemEnderPearl);
        targets.put("Potion", item -> item instanceof ItemPotion);
        targets.put("Throwable", item -> item instanceof ItemEgg || item instanceof ItemSnowball);
        targets.put("FishingRod", item -> item instanceof ItemFishingRod);
        targets.put("TNT", item -> item == Item.getItemFromBlock(Blocks.tnt));
        targets.put("Shears", item -> item instanceof ItemShears);
        targets.put("Ignore", null);
        return targets;
    }

    private static int identityIndexOf(List<ItemStack> stacks, ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i) == stack) return i;
        }
        return -1;
    }

    public boolean handleEventsPublic() {
        return isToggle() && mc.thePlayer != null && mc.theWorld != null;
    }

    public boolean isSortEnabled() {
        return sort.isEnabled();
    }
}
