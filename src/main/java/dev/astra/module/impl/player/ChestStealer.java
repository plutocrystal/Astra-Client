package dev.astra.module.impl.player;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.packet.PacketReceiveEvent;
import dev.astra.event.events.render.Render2DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import dev.astra.util.RandomUtils;
import dev.astra.util.RenderUtil;
import dev.astra.util.inventory.LBInventoryManager;
import dev.astra.util.inventory.LBInventoryUtils;
import dev.astra.util.inventory.SilentHotbar;
import dev.astra.util.inventory.TickedActions;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S30PacketWindowItems;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChestStealer extends Module {
    private final BooleanValue smartDelay = new BooleanValue("SmartDelay", false);

    private final NumberValue multiplier = new NumberValue("DelayMultiplier", 120, 0, 500, 1);

    private final BooleanValue smartOrder = new BooleanValue("SmartOrder", true);

    private final BooleanValue simulateShortStop = new BooleanValue("SimulateShortStop", false);

    private final RangeSelectionNumberValue delay =
            new RangeSelectionNumberValue("Delay", 50.0, 50.0, 0.0, 500.0, 1.0);

    private final RangeSelectionNumberValue startDelay =
            new RangeSelectionNumberValue("StartDelay", 50.0, 100.0, 0.0, 500.0, 1.0);

    private final RangeSelectionNumberValue closeDelay =
            new RangeSelectionNumberValue("CloseDelay", 50.0, 100.0, 0.0, 500.0, 1.0);

    public final BooleanValue noMove = LBInventoryManager.noMoveValue;

    public final BooleanValue noMoveAir = LBInventoryManager.noMoveAirValue;

    public final BooleanValue noMoveGround = LBInventoryManager.noMoveGroundValue;

    private final BooleanValue chestTitle = new BooleanValue("ChestTitle", true);

    private final BooleanValue randomSlot = new BooleanValue("RandomSlot", true);

    private final BooleanValue progressBar = new BooleanValue("ProgressBar", true);

    private final BooleanValue silentGUI = new BooleanValue("SilentGUI", false);

    private final BooleanValue highlightSlot = new BooleanValue("Highlight-Slot", false);

    private final ColorValue backgroundColor = new ColorValue("BackgroundColor", new Color(128, 128, 128, 128).getRGB());

    private final NumberValue borderStrength = new NumberValue("Border-Strength", 3, 1, 5, 1);

    private final ColorValue borderColor = new ColorValue("BorderColor", new Color(128, 128, 128).getRGB());

    private final ModeValue chestDebug = new ModeValue("Chest-Debug", "Off", "Off", "Text");

    private final BooleanValue itemStolenDebug = new BooleanValue("ItemStolen-Debug", false);

    private static class ItemTakeRecord {
        final int index;
        final ItemStack stack;
        final Integer sortableTo;
        ItemTakeRecord(int index, ItemStack stack, Integer sortableTo) {
            this.index = index;
            this.stack = stack;
            this.sortableTo = sortableTo;
        }
    }

    private enum StealPhase {
        IDLE, START_DELAY, ROUND, ITERATE, AWAIT_ROUND, HAS_TAKEN_CLOSE, CLOSE_NEXT_TICK, FINAL_AWAIT
    }

    private StealPhase phase = StealPhase.IDLE;

    private List<ItemTakeRecord> itemsToSteal = new ArrayList<>();

    private int iterateCursor = 0;

    private boolean hasTaken = false;

    private Float progress = null;

    private float easingProgress = 0.0f;

    private Integer receivedId = null;

    private long idMismatchSince = 0L;

    private static final long ID_MISMATCH_TIMEOUT = 1000L;

    private ItemStack[] packetStacks = new ItemStack[0];

    public ChestStealer() {
        super("ChestStealer", 0, Category.PLAYER);
        addValues(smartDelay, multiplier, smartOrder,
                simulateShortStop,
                delay, startDelay, closeDelay,
                noMove, noMoveAir, noMoveGround,
                chestTitle, randomSlot, progressBar, silentGUI,
                highlightSlot, backgroundColor, borderStrength, borderColor,
                chestDebug, itemStolenDebug);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        TickedActions.clearTicked(this);
        resetState();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    private void resetState() {
        phase = StealPhase.IDLE;
        itemsToSteal = new ArrayList<>();
        iterateCursor = 0;
        hasTaken = false;
        progress = null;
        easingProgress = 0.0f;
        receivedId = null;
        idMismatchSince = 0L;
        LBInventoryManager.chestStealerCurrentSlot = -1;
        LBInventoryManager.chestStealerLastSlot = -1;
    }

    public void refreshVisibility() {
        multiplier.setVisible(smartDelay.isEnabled());
        smartOrder.setVisible(smartDelay.isEnabled());
        delay.setVisible(!smartDelay.isEnabled());
        highlightSlot.setVisible(!silentGUI.isEnabled());
        boolean colorsVisible = highlightSlot.isEnabled() && !silentGUI.isEnabled();
        backgroundColor.setVisible(colorsVisible);
        borderStrength.setVisible(colorsVisible);
        borderColor.setVisible(colorsVisible);
        itemStolenDebug.setVisible(!"Off".equals(chestDebug.getValue()));
    }

    private boolean handleEvents() {
        return isToggle() && mc.thePlayer != null && mc.theWorld != null;
    }

    private void debug(String message) {
        if (!"Text".equals(chestDebug.getValue())) return;
        ChatUtil.sendMessage("\u00a78[\u00a79\u00a7lChestStealer\u00a78] \u00a73" + message);
    }

    private long sampleRange(RangeSelectionNumberValue range) {
        double min = Math.max(0.0, range.getMinValue());
        double max = Math.max(min, range.getMaxValue());
        return (long) RandomUtils.nextDouble(min, max);
    }

    private long sampleSmartDelay(int fromIndex, int toIndex) {
        double min = Math.max(0.0D, delay.getMinValue());
        double max = Math.max(min, delay.getMaxValue());
        int dist = squaredDistanceOfSlots(fromIndex, toIndex);
        long base = (long) (Math.sqrt(dist) * multiplier.getValue());
        long spread = Math.max(1L, base / 4L);
        long low = Math.max((long) min, Math.min((long) max, base - spread));
        long high = Math.max((long) min, Math.min((long) max, base + spread));
        return RandomUtils.nextLong(low, high);
    }

    private long delayMaxValue() {
        double min = Math.max(0.0D, delay.getMinValue());
        return (long) Math.max(min, delay.getMaxValue());
    }

    public boolean shouldHighlight() {
        return highlightSlot.isEnabled() && !silentGUI.isEnabled();
    }

    public int getHighlightBackgroundColor() {
        return backgroundColor.getValue();
    }

    public int getHighlightBorderColor() {
        return borderColor.getValue();
    }

    public float getHighlightBorderStrength() {
        return (float) Math.round(borderStrength.getValue());
    }

    private static final int OP_OK = 0;

    private static final int OP_WAIT = 1;

    private static final int OP_STOP = 2;

    private int shouldOperate() {
        if (!handleEvents()) {
            return OP_STOP;
        }
        if (mc.playerController == null || !mc.playerController.getCurrentGameType().isSurvivalOrAdventure()) {
            return OP_STOP;
        }
        if (!(mc.currentScreen instanceof GuiChest)) {
            idMismatchSince = 0L;
            return OP_STOP;
        }
        if (mc.thePlayer.openContainer == null || receivedId == null
                || mc.thePlayer.openContainer.windowId != receivedId) {
            long now = System.currentTimeMillis();
            if (idMismatchSince == 0L) {
                idMismatchSince = now;
            } else if (now - idMismatchSince >= ID_MISMATCH_TIMEOUT && mc.thePlayer.openContainer != null) {
                receivedId = mc.thePlayer.openContainer.windowId;
                idMismatchSince = 0L;
            }
            LBInventoryManager.delay(50);
            return OP_WAIT;
        }
        idMismatchSince = 0L;
        if (LBInventoryManager.canClickInventory(false)) {
            return OP_OK;
        }
        LBInventoryManager.delay(50);
        return OP_WAIT;
    }

    public boolean stealStep() {
        switch (phase) {
            case IDLE: {
                if (!handleEvents()) {
                    return true;
                }
                int op = shouldOperate();
                if (op == OP_STOP) {
                    return true;
                }
                if (op == OP_WAIT) {
                    return false;
                }
                if (chestTitle.isEnabled()) {
                    if (!(mc.thePlayer.openContainer instanceof ContainerChest)) {
                        return true;
                    }
                    ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                    String inventoryName = chest.getLowerChestInventory().getName();
                    if (inventoryName == null || !inventoryName.contains(Blocks.chest.getLocalizedName())) {
                        return true;
                    }
                }
                setProgress(0.0f);
                phase = StealPhase.START_DELAY;
                return false;
            }
            case START_DELAY: {
                LBInventoryManager.delay(sampleRange(startDelay));
                debug("Stealing items..");
                phase = StealPhase.ROUND;
                return false;
            }
            case ROUND: {
                int op = shouldOperate();
                if (op == OP_STOP) {
                    scheduleCleanup();
                    return true;
                }
                if (op == OP_WAIT) {
                    return false;
                }
                hasTaken = false;
                itemsToSteal = getItemsToSteal();
                iterateCursor = 0;
                phase = StealPhase.ITERATE;
                return false;
            }
            case ITERATE: {
                if (iterateCursor >= itemsToSteal.size()) {
                    phase = StealPhase.AWAIT_ROUND;
                    return false;
                }
                int op = shouldOperate();
                if (op == OP_STOP) {
                    TickedActions.nextTick(this, () -> SilentHotbar.resetSlot(null, false));
                    LBInventoryManager.chestStealerLastSlot = -1;
                    LBInventoryManager.chestStealerCurrentSlot = -1;
                    return true;
                }
                if (op == OP_WAIT) {
                    return false;
                }
                ItemTakeRecord record = itemsToSteal.get(iterateCursor);
                hasTaken = true;
                LBInventoryManager.chestStealerLastSlot = LBInventoryManager.chestStealerCurrentSlot;
                LBInventoryManager.chestStealerCurrentSlot = record.index;
                long stealingDelay;
                if (smartDelay.isEnabled() && iterateCursor + 1 < itemsToSteal.size()) {
                    stealingDelay = sampleSmartDelay(record.index,
                            itemsToSteal.get(iterateCursor + 1).index);
                } else {
                    stealingDelay = sampleRange(delay);
                }
                if (itemStolenDebug.isEnabled()) {
                    debug("item: " + record.stack.getDisplayName().toLowerCase() + " | slot: "
                            + record.index + " | delay: " + stealingDelay + "ms");
                }
                final ItemTakeRecord finalRecord = record;
                final int itemPosition = iterateCursor;
                final int totalItems = itemsToSteal.size();
                TickedActions.clickNextTick(this, record.index,
                        record.sortableTo == null ? 0 : record.sortableTo,
                        record.sortableTo != null ? 2 : 1,
                        false,
                        mc.thePlayer.openContainer.windowId,
                        clickedStack -> {
                            setProgress((itemPosition + 1) / (float) totalItems);
                            AutoArmor autoArmor = Main.moduleManager == null ? null
                                    : Main.moduleManager.getModuleByClass(AutoArmor.class);
                            if (autoArmor == null || !autoArmor.canEquipFromChest()) {
                                return;
                            }
                            if (!(finalRecord.stack.getItem() instanceof ItemArmor)) {
                                return;
                            }
                            int armorPos = EntityLiving.getArmorPosition(finalRecord.stack) - 1;
                            if (armorPos < 0 || armorPos >= 4) return;
                            if (mc.thePlayer != null && mc.thePlayer.inventory.armorInventory[armorPos] != null) {
                                return;
                            }
                            TickedActions.nextTick(ChestStealer.this, () -> {
                                if (mc.thePlayer == null) return;
                                ItemStack[] hotbarStacks = new ItemStack[9];
                                System.arraycopy(mc.thePlayer.inventory.mainInventory, 0, hotbarStacks, 0, 9);
                                int newIndex = -1;
                                for (int s = 0; s < hotbarStacks.length; s++) {
                                    if (hotbarStacks[s] != null
                                            && ItemStack.areItemStacksEqual(hotbarStacks[s], finalRecord.stack)) {
                                        newIndex = s;
                                        break;
                                    }
                                }
                                if (newIndex != -1) {
                                    autoArmor.equipFromHotbarInChest(newIndex, finalRecord.stack);
                                }
                            });
                        });
                long totalDelay = stealingDelay;
                if (simulateShortStop.isEnabled() && RandomUtils.nextDouble() > 0.75D) {
                    long pauseBase = delayMaxValue();
                    totalDelay += RandomUtils.nextLong(pauseBase, pauseBase * 4L);
                }
                LBInventoryManager.delay(totalDelay);
                iterateCursor++;
                return false;
            }
            case AWAIT_ROUND: {
                if (!TickedActions.isEmpty(this)) {
                    return false;
                }
                if (!hasTaken) {
                    setProgress(1.0f);
                    LBInventoryManager.delay(sampleRange(closeDelay));
                    phase = StealPhase.CLOSE_NEXT_TICK;
                    return false;
                }
                refreshPacketStacksFromContainer();
                phase = StealPhase.ROUND;
                return false;
            }
            case CLOSE_NEXT_TICK: {
                if (!TickedActions.isEmpty(this)) {
                    return false;
                }
                TickedActions.nextTick(this, () -> {
                    LBInventoryManager.chestStealerLastSlot = -1;
                    LBInventoryManager.chestStealerCurrentSlot = -1;
                    if (mc.thePlayer != null) {
                        mc.thePlayer.closeScreen();
                    }
                    setProgress(null);
                    debug("Chest closed");
                });
                phase = StealPhase.FINAL_AWAIT;
                return false;
            }
            case FINAL_AWAIT: {
                if (!TickedActions.isEmpty(this)) {
                    return false;
                }
                resetState();
                return true;
            }
            default:
                resetState();
                return true;
        }
    }

    private void scheduleCleanup() {
        TickedActions.nextTick(this, () -> SilentHotbar.resetSlot(null, false));
        LBInventoryManager.chestStealerLastSlot = -1;
        LBInventoryManager.chestStealerCurrentSlot = -1;
    }

    private void setProgress(Float value) {
        if (value == null) {
            progress = null;
            easingProgress = 0.0f;
            return;
        }
        progress = Math.max(0.0f, Math.min(1.0f, value));
    }

    private void refreshPacketStacksFromContainer() {
        if (mc.thePlayer == null || mc.thePlayer.openContainer == null) return;
        List<ItemStack> inventory = mc.thePlayer.openContainer.getInventory();
        packetStacks = inventory.toArray(new ItemStack[0]);
    }

    private int squaredDistanceOfSlots(int from, int to) {
        int x1 = from % 9;
        int y1 = from / 9;
        int x2 = to % 9;
        int y2 = to / 9;
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    private List<ItemStack> buildEvalStacks() {
        if (mc.thePlayer != null && mc.thePlayer.openContainer != null) {
            List<ItemStack> live = mc.thePlayer.openContainer.getInventory();
            if (live != null && live.size() > 36 && live.size() == packetStacks.length) {
                return live;
            }
        }
        List<ItemStack> fallback = new ArrayList<>();
        Collections.addAll(fallback, packetStacks);
        return fallback;
    }

    private List<ItemTakeRecord> getItemsToSteal() {
        List<ItemStack> stacks = buildEvalStacks();
        boolean[] sortBlacklist = new boolean[9];
        int spaceInInventory = LBInventoryUtils.countSpaceInInventory();
        InventoryCleaner cleaner = Main.moduleManager == null ? null
                : Main.moduleManager.getModuleByClass(InventoryCleaner.class);
        boolean cleanerActive = cleaner != null && cleaner.isToggle() && cleaner.handleEventsPublic();
        List<ItemTakeRecord> records = new ArrayList<>();
        int chestAreaSize = stacks.size() - 36;
        if (chestAreaSize <= 0) {
            return finishRecords(records);
        }
        for (int index = 0; index < chestAreaSize; index++) {
            ItemStack stack = stacks.get(index);
            if (stack == null) continue;
            if (TickedActions.isTicked(this, index)) continue;
            int mergeableCount = 0;
            for (ItemStack otherStack : mc.thePlayer.inventory.mainInventory) {
                if (otherStack == null) continue;
                if (otherStack.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(stack, otherStack)) {
                    mergeableCount += otherStack.getMaxStackSize() - otherStack.stackSize;
                }
            }
            boolean canMerge = mergeableCount > 0;
            boolean canFullyMerge = mergeableCount >= stack.stackSize;
            if (!canMerge && spaceInInventory <= 0) continue;
            if (cleanerActive && !cleaner.isStackUseful(stack, stacks, null, canFullyMerge, false)) {
                continue;
            }
            Integer sortableTo = null;
            if (!canMerge && cleanerActive && cleaner.isSortEnabled()) {
                for (int hotbarIndex = 0; hotbarIndex <= 8; hotbarIndex++) {
                    if (sortBlacklist[hotbarIndex]) continue;
                    if (!cleaner.canBeSortedTo(hotbarIndex, stack.getItem())) continue;
                    int hotbarStackIndex = stacks.size() - 9 + hotbarIndex;
                    ItemStack hotbarStack = hotbarStackIndex >= 0 && hotbarStackIndex < stacks.size()
                            ? stacks.get(hotbarStackIndex) : null;
                    if (!cleaner.canBeSortedTo(hotbarIndex,
                            hotbarStack == null ? null : hotbarStack.getItem())
                            || !cleaner.isStackUseful(hotbarStack, stacks, null, false, true)) {
                        sortableTo = hotbarIndex;
                        sortBlacklist[hotbarIndex] = true;
                        break;
                    }
                }
            }
            if (!canFullyMerge) spaceInInventory--;
            records.add(new ItemTakeRecord(index, stack, sortableTo));
        }
        return finishRecords(records);
    }

    private List<ItemTakeRecord> finishRecords(List<ItemTakeRecord> records) {
        if (randomSlot.isEnabled()) {
            Collections.shuffle(records);
        }
        records.sort((a, b) -> Boolean.compare(
                b.stack.getItem() instanceof ItemArmor,
                a.stack.getItem() instanceof ItemArmor));
        records.sort((a, b) -> Boolean.compare(b.sortableTo != null, a.sortableTo != null));
        AutoArmor autoArmor = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(AutoArmor.class);
        if (autoArmor != null && autoArmor.canEquipFromChest()) {
            records.sort((a, b) -> Boolean.compare(
                    b.stack.getItem() instanceof ItemArmor,
                    a.stack.getItem() instanceof ItemArmor));
        }
        if (smartOrder.isEnabled()) {
            sortBasedOnOptimumPath(records);
        }
        return records;
    }

    private void sortBasedOnOptimumPath(List<ItemTakeRecord> itemsToSteal) {
        for (int i = 0; i < itemsToSteal.size(); i++) {
            int minDistance = Integer.MAX_VALUE;
            for (int j = i + 1; j < itemsToSteal.size(); j++) {
                int distance = squaredDistanceOfSlots(itemsToSteal.get(i).index, itemsToSteal.get(j).index);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
            double threshold = Math.max(minDistance * 1.6D, 1.0D);
            List<Integer> candidates = new ArrayList<>();
            for (int j = i + 1; j < itemsToSteal.size(); j++) {
                int distance = squaredDistanceOfSlots(itemsToSteal.get(i).index, itemsToSteal.get(j).index);
                if (distance <= threshold) {
                    candidates.add(j);
                }
            }
            if (candidates.isEmpty()) continue;
            Integer pickIndex = RandomUtils.getRandomElement(candidates);
            if (pickIndex == null || pickIndex.intValue() == i + 1) continue;
            ItemTakeRecord picked = itemsToSteal.get(pickIndex.intValue());
            itemsToSteal.set(pickIndex.intValue(), itemsToSteal.get(i + 1));
            itemsToSteal.set(i + 1, picked);
        }
    }

    @EventHandler
    public void onPacket(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacket() instanceof C0DPacketCloseWindow
                || event.getPacket() instanceof S2DPacketOpenWindow
                || event.getPacket() instanceof S2EPacketCloseWindow) {
            receivedId = null;
            setProgress(null);
        } else if (event.getPacket() instanceof S30PacketWindowItems) {
            int packetWindowId = ((S30PacketWindowItems) event.getPacket()).func_148911_c();
            if (packetWindowId == 0) return;
            if (receivedId == null || receivedId != packetWindowId) {
                debug("Chest opened with " + packetStacks.length + " items");
            }
            receivedId = packetWindowId;
            idMismatchSince = 0L;
            packetStacks = ((S30PacketWindowItems) event.getPacket()).getItemStacks();
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!progressBar.isEnabled() || !(mc.currentScreen instanceof GuiChest)) return;
        Float current = progress;
        if (current == null) return;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float scaledWidth = scaledResolution.getScaledWidth();
        float scaledHeight = scaledResolution.getScaledHeight();
        float minX = scaledWidth * 0.3f;
        float maxX = scaledWidth * 0.7f;
        float minY = scaledHeight * 0.75f;
        float maxY = minY + 10.0f;
        float partialTicks = mc.getTimer() != null ? mc.getTimer().renderPartialTicks : 0.5f;
        easingProgress += (current - easingProgress) / 6.0f * partialTicks;
        easingProgress = Math.max(0.0f, Math.min(1.0f, easingProgress));
        RenderUtil.drawRect(minX - 2, minY - 2, maxX + 2, maxY + 2, new Color(200, 200, 200).getRGB());
        RenderUtil.drawRect(minX, minY, maxX, maxY, new Color(50, 50, 50).getRGB());
        RenderUtil.drawRect(minX, minY, minX + (maxX - minX) * easingProgress, maxY,
                Color.HSBtoRGB(easingProgress / 5.0f, 1.0f, 1.0f));
    }
}
