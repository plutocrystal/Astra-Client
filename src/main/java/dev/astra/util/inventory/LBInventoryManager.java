package dev.astra.util.inventory;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.module.impl.player.AutoArmor;
import dev.astra.module.impl.player.ChestStealer;
import dev.astra.module.impl.player.InventoryCleaner;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ColorValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import java.awt.Color;
import java.util.List;

public final class LBInventoryManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static boolean initialized = false;

    public static final BooleanValue noMoveValue = new BooleanValue("NoMoveClicks", false);

    public static final BooleanValue noMoveAirValue = new BooleanValue("NoClicksInAir", false);

    public static final BooleanValue noMoveGroundValue = new BooleanValue("NoClicksOnGround", true);

    public static final BooleanValue invOpenValue = new BooleanValue("InvOpen", false);

    public static final BooleanValue simulateInventoryValue = new BooleanValue("SimulateInventory", true);

    public static final BooleanValue autoCloseValue = new BooleanValue("AutoClose", false);

    public static final NumberValue postInventoryCloseDelayValue = new NumberValue("PostInventoryCloseDelay", 0, 0, 500, 1);

    public static final NumberValue startDelayValue = new NumberValue("StartDelay", 0, 0, 500, 1);

    public static final NumberValue closeDelayValue = new NumberValue("CloseDelay", 0, 0, 500, 1);

    public static final BooleanValue highlightSlotValue = new BooleanValue("Highlight-Slot", false);

    public static final ColorValue backgroundColorValue = new ColorValue("BackgroundColor", new Color(128, 128, 128).getRGB());

    public static final NumberValue borderStrengthValue = new NumberValue("Border-Strength", 3, 1, 5, 1);

    public static final ColorValue borderColorValue = new ColorValue("BorderColor", new Color(128, 128, 128).getRGB());

    public static boolean hasScheduledInLastLoop = false;

    private static boolean canCloseInventory = false;

    public static int chestStealerCurrentSlot = -1;

    public static int chestStealerLastSlot = -1;

    public static int invCleanerCurrentSlot = -1;

    public static int invCleanerLastSlot = -1;

    public static int autoArmorCurrentSlot = -1;

    public static int autoArmorLastSlot = -1;

    private enum Phase {
        STEAL, ARMOR_HOTBAR, GATE, ARMOR_INV, REPAIR, MERGE, SORT, DROP, CLOSE_DECIDE, CLOSE_RUN
    }

    private static Phase phase = Phase.STEAL;

    private static boolean delaying = false;

    private static long delayUntil = 0L;

    private static Runnable pendingClosingAction = null;

    private LBInventoryManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        Main.eventBus.register(new PreUpdateBridge());
        Main.eventBus.register(new VisibilityBridge());
        Main.eventBus.register(new LBInventoryUtils.PacketListener());
        refreshVisibility();
    }

    public static class PreUpdateBridge {
        @EventHandler
        public void onPreUpdate(PreUpdateEvent event) {
            tick();
        }
    }

    public static class VisibilityBridge {
        @EventHandler
        public void onTick(TickEvent event) {
            refreshVisibility();
        }
    }

    public static void markScheduled() {
        hasScheduledInLastLoop = true;
        canCloseInventory = true;
    }

    public static void delay(long ms) {
        delaying = true;
        delayUntil = System.currentTimeMillis() + ms;
    }

    public static boolean isMoving() {
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null) return false;
        return mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F;
    }

    public static boolean canClickInventory(boolean closeWhenViolating) {
        boolean ground = mc.thePlayer != null && mc.thePlayer.onGround;
        boolean noMoveApplies = ground ? noMoveGroundValue.isEnabled() : noMoveAirValue.isEnabled();
        if (noMoveValue.isEnabled() && isMoving() && noMoveApplies) {
            if (closeWhenViolating) {
                LBInventoryUtils.setServerOpenInventory(false);
            }
            return false;
        }
        return true;
    }

    private static void tick() {
        long now = System.currentTimeMillis();
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        TickedActions.tick();
        SilentHotbar.updateSilentSlot();
        if (delaying) {
            if (now < delayUntil) {
                return;
            }
            delaying = false;
        }
        ChestStealer stealer = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(ChestStealer.class);
        AutoArmor armor = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(AutoArmor.class);
        InventoryCleaner cleaner = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(InventoryCleaner.class);
        int safety = 64;
        boolean waiting = false;
        while (safety-- > 0 && !delaying && !waiting) {
            switch (phase) {
                case STEAL: {
                    if (stealer == null || !stealer.isToggle()) {
                        phase = Phase.ARMOR_HOTBAR;
                    } else if (!stealer.stealStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.ARMOR_HOTBAR;
                    }
                    break;
                }
                case ARMOR_HOTBAR: {
                    if (armor == null || !armor.isToggle()) {
                        phase = Phase.GATE;
                    } else if (!armor.equipFromHotbarStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.GATE;
                    }
                    break;
                }
                case GATE: {
                    if (!canClickInventory(false)
                            || (invOpenValue.isEnabled() && !(mc.currentScreen instanceof GuiInventory))) {
                        delay(50);
                        phase = Phase.STEAL;
                    } else {
                        canCloseInventory = false;
                        phase = Phase.ARMOR_INV;
                    }
                    break;
                }
                case ARMOR_INV: {
                    if (armor == null || !armor.isToggle()) {
                        phase = Phase.REPAIR;
                    } else if (!armor.equipFromInventoryStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.REPAIR;
                    }
                    break;
                }
                case REPAIR: {
                    if (cleaner == null || !cleaner.isToggle()) {
                        phase = Phase.MERGE;
                    } else if (!cleaner.repairStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.MERGE;
                    }
                    break;
                }
                case MERGE: {
                    if (cleaner == null || !cleaner.isToggle()) {
                        phase = Phase.SORT;
                    } else if (!cleaner.mergeStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.SORT;
                    }
                    break;
                }
                case SORT: {
                    if (cleaner == null || !cleaner.isToggle()) {
                        phase = Phase.DROP;
                    } else if (!cleaner.sortStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.DROP;
                    }
                    break;
                }
                case DROP: {
                    if (cleaner == null || !cleaner.isToggle()) {
                        phase = Phase.CLOSE_DECIDE;
                    } else if (!cleaner.dropStep()) {
                        waiting = true;
                    } else {
                        phase = Phase.CLOSE_DECIDE;
                    }
                    break;
                }
                case CLOSE_DECIDE: {
                    Runnable action = closingAction();
                    if (action == null) {
                        delay(50);
                        phase = Phase.STEAL;
                    } else {
                        pendingClosingAction = action;
                        delay(closeDelayValue.getValue().longValue());
                        phase = Phase.CLOSE_RUN;
                    }
                    break;
                }
                case CLOSE_RUN: {
                    if (!hasScheduledInLastLoop && pendingClosingAction != null) {
                        pendingClosingAction.run();
                    }
                    pendingClosingAction = null;
                    phase = Phase.STEAL;
                    break;
                }
            }
        }
        if (phase == Phase.STEAL) {
            hasScheduledInLastLoop = false;
        }
    }

    private static Runnable closingAction() {
        if (!canCloseInventory) {
            return null;
        }
        if (mc.thePlayer == null || mc.thePlayer.openContainer == null || mc.thePlayer.openContainer.windowId != 0) {
            return null;
        }
        if (mc.currentScreen instanceof GuiInventory && invOpenValue.isEnabled() && autoCloseValue.isEnabled()) {
            return () -> {
                if (mc.thePlayer != null) {
                    mc.thePlayer.closeScreen();
                }
            };
        }
        if (!(mc.currentScreen instanceof GuiInventory) && simulateInventoryValue.isEnabled()
                && LBInventoryUtils.isServerOpenInventory()) {
            return () -> LBInventoryUtils.setServerOpenInventory(false);
        }
        return null;
    }

    public static void resetState() {
        phase = Phase.STEAL;
        delaying = false;
        pendingClosingAction = null;
        hasScheduledInLastLoop = false;
        canCloseInventory = false;
        chestStealerCurrentSlot = chestStealerLastSlot = -1;
        invCleanerCurrentSlot = invCleanerLastSlot = -1;
        autoArmorCurrentSlot = autoArmorLastSlot = -1;
    }

    public static void onWorldChange() {
        TickedActions.onWorldChange();
        LBInventoryUtils.onWorldChange();
        resetState();
    }

    public static void refreshVisibility() {
        noMoveAirValue.setVisible(noMoveValue.isEnabled());
        noMoveGroundValue.setVisible(noMoveValue.isEnabled());
        simulateInventoryValue.setVisible(!invOpenValue.isEnabled());
        autoCloseValue.setVisible(invOpenValue.isEnabled());
        boolean delaysVisible = invOpenValue.isEnabled() || simulateInventoryValue.isEnabled();
        startDelayValue.setVisible(delaysVisible);
        closeDelayValue.setVisible(invOpenValue.isEnabled() ? autoCloseValue.isEnabled() : simulateInventoryValue.isEnabled());
        backgroundColorValue.setVisible(highlightSlotValue.isEnabled());
        borderStrengthValue.setVisible(highlightSlotValue.isEnabled());
        borderColorValue.setVisible(highlightSlotValue.isEnabled());
        if (Main.moduleManager != null) {
            InventoryCleaner cleaner = Main.moduleManager.getModuleByClass(InventoryCleaner.class);
            if (cleaner != null) {
                cleaner.refreshVisibility();
            }
            AutoArmor armor = Main.moduleManager.getModuleByClass(AutoArmor.class);
            if (armor != null) {
                armor.refreshVisibility();
            }
            ChestStealer stealer = Main.moduleManager.getModuleByClass(ChestStealer.class);
            if (stealer != null) {
                stealer.refreshVisibility();
            }
        }
    }

    public static void renderHighlights(GuiContainer gui) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        ChestStealer stealer = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(ChestStealer.class);
        AutoArmor armor = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(AutoArmor.class);
        InventoryCleaner cleaner = Main.moduleManager == null ? null : Main.moduleManager.getModuleByClass(InventoryCleaner.class);
        List<Slot> slots = gui.inventorySlots.inventorySlots;
        if (stealer != null && stealer.isToggle() && stealer.shouldHighlight()) {
            drawSlotHighlight(slots, chestStealerCurrentSlot,
                    stealer.getHighlightBackgroundColor(),
                    stealer.getHighlightBorderColor(),
                    stealer.getHighlightBorderStrength());
        }
        if (cleaner != null && cleaner.isToggle() && highlightSlotValue.isEnabled()) {
            drawSlotHighlight(slots, invCleanerCurrentSlot,
                    backgroundColorValue.getValue(), borderColorValue.getValue(),
                    (float) Math.round(borderStrengthValue.getValue()));
        }
        if (armor != null && armor.isToggle() && highlightSlotValue.isEnabled()) {
            drawSlotHighlight(slots, autoArmorCurrentSlot,
                    backgroundColorValue.getValue(), borderColorValue.getValue(),
                    (float) Math.round(borderStrengthValue.getValue()));
        }
    }

    private static void drawSlotHighlight(List<Slot> slots, int index, int background, int border, float strength) {
        if (index < 0 || index >= slots.size()) {
            return;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return;
        }
        float x = slot.xDisplayPosition;
        float y = slot.yDisplayPosition;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        dev.astra.util.RenderUtil.drawRect(x, y, x + 16.0f, y + 16.0f, background);
        dev.astra.util.RenderUtil.drawRect(x - strength, y - strength, x + 16.0f + strength, y, border);
        dev.astra.util.RenderUtil.drawRect(x - strength, y + 16.0f, x + 16.0f + strength, y + 16.0f + strength, border);
        dev.astra.util.RenderUtil.drawRect(x - strength, y, x, y + 16.0f, border);
        dev.astra.util.RenderUtil.drawRect(x + 16.0f, y, x + 16.0f + strength, y + 16.0f, border);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
