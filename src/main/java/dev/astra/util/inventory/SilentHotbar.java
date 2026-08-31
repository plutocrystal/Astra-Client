package dev.astra.util.inventory;

import net.minecraft.client.Minecraft;

public final class SilentHotbar {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static SilentHotbarState hotbarState = null;

    private static int ticksSinceLastUpdate = 0;

    private static Integer originalSlot = null;

    public static boolean ignoreSlotChange = false;

    public static boolean pressedAtSlot = false;

    private SilentHotbar() {
    }

    public static int getCurrentSlot() {
        if (hotbarState != null) {
            return hotbarState.enforcedSlot;
        }
        if (mc.thePlayer != null && mc.thePlayer.inventory != null) {
            return mc.thePlayer.inventory.currentItem;
        }
        return 0;
    }

    public static boolean isActive() {
        return hotbarState != null;
    }

    public static boolean isModifiedThisTick() {
        return ticksSinceLastUpdate == 0 && hotbarState != null;
    }

    public static void selectSlotSilently(Object requester, int slot, Integer ticksUntilReset,
                                          boolean immediate, boolean render, boolean resetManually) {
        if (originalSlot == null) {
            originalSlot = mc.thePlayer != null && mc.thePlayer.inventory != null ? mc.thePlayer.inventory.currentItem : 0;
        }
        hotbarState = new SilentHotbarState(slot, requester, ticksUntilReset, render, resetManually);
        ticksSinceLastUpdate = 0;
        if (immediate) {
            if (mc.playerController != null) {
                mc.playerController.syncCurrentPlayItem();
            }
        }
    }

    public static void selectSlotSilently(Object requester, int slot) {
        selectSlotSilently(requester, slot, null, false, true, false);
    }

    public static void resetSlot(Object requester, boolean immediate) {
        SilentHotbarState state = hotbarState;
        if (state == null) {
            return;
        }
        if (requester == null || state.requester == requester) {
            hotbarState = null;
            originalSlot = null;
            if (requester != null && immediate) {
                if (mc.playerController != null) {
                    mc.playerController.syncCurrentPlayItem();
                }
            }
        }
    }

    public static void resetSlot() {
        resetSlot(null, false);
    }

    public static boolean isSlotModified(Object requester) {
        return hotbarState != null && hotbarState.requester == requester;
    }

    public static void updateSilentSlot() {
        pressedAtSlot = false;
        SilentHotbarState state = hotbarState;
        if (state == null) {
            return;
        }
        Integer ticksUntilReset = state.resetTicks;
        if (ticksUntilReset != null) {
            if (ticksSinceLastUpdate >= ticksUntilReset) {
                resetSlot(state.requester, false);
                return;
            }
        }
        ticksSinceLastUpdate++;
    }

    public static int renderSlot(boolean option) {
        if (mc.thePlayer == null) {
            return 0;
        }
        int original = mc.thePlayer.inventory.currentItem;
        SilentHotbarState state = hotbarState;
        if (state == null) {
            return original;
        }
        return option || state.render ? getCurrentSlot() : original;
    }

    private static boolean shouldReset(int slot, Integer other, boolean keyPressCheck) {
        Integer otherSlot = other != null ? other : originalSlot;
        boolean differs = otherSlot == null || slot != otherSlot;
        return (differs || keyPressCheck) && hotbarState != null && hotbarState.resetManually;
    }

    public static boolean handleServerSlotChange(int slot) {
        if (ignoreSlotChange) {
            ignoreSlotChange = false;
            originalSlot = null;
            return true;
        }
        if (shouldReset(slot, null, pressedAtSlot)) {
            resetSlot(null, false);
            return false;
        }
        return hotbarState != null;
    }

    public static void handleSentSlotPacket(int slot) {
        if (shouldReset(slot, getCurrentSlot(), false)) {
            resetSlot(null, false);
        }
    }

    public static void onWorldChange() {
        resetSlot(null, false);
        ignoreSlotChange = false;
        pressedAtSlot = false;
    }

    public static class SilentHotbarState {
        public final int enforcedSlot;
        public final Object requester;
        public final Integer resetTicks;
        public final boolean render;
        public final boolean resetManually;
        public SilentHotbarState(int enforcedSlot, Object requester, Integer resetTicks,
                                 boolean render, boolean resetManually) {
            this.enforcedSlot = enforcedSlot;
            this.requester = requester;
            this.resetTicks = resetTicks;
            this.render = render;
            this.resetManually = resetManually;
        }
    }
}
