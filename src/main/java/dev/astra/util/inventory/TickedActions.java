package dev.astra.util.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class TickedActions {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Queue<Action> actions = new ConcurrentLinkedQueue<>();

    private static final Set<Action> calledThisTick = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private TickedActions() {
    }

    private static class Action {
        final Object owner;
        final int id;
        final Runnable runnable;
        Action(Object owner, int id, Runnable runnable) {
            this.owner = owner;
            this.id = id;
            this.runnable = runnable;
        }
    }

    public static boolean schedule(Object owner, int id, boolean allowDuplicates, Runnable action) {
        if (allowDuplicates || !isScheduled(owner, id)) {
            actions.add(new Action(owner, id, action));
            return true;
        }
        return false;
    }

    private static boolean isScheduled(Object owner, int id) {
        for (Action action : actions) {
            if (action.owner == owner && action.id == id && !calledThisTick.contains(action)) {
                return true;
            }
        }
        return false;
    }

    public static void clear(Object owner) {
        actions.removeIf(action -> action.owner == owner);
    }

    public static void clearAll() {
        actions.clear();
        calledThisTick.clear();
    }

    public static int size(Object owner) {
        int count = 0;
        for (Action action : actions) {
            if (action.owner == owner) count++;
        }
        return count;
    }

    public static boolean isEmpty(Object owner) {
        return size(owner) == 0;
    }

    public static void tick() {
        calledThisTick.addAll(actions);
        for (Action action : calledThisTick) {
            actions.remove(action);
            try {
                action.runnable.run();
            } catch (Throwable t) {
                System.err.println("[Astra] TickedActions action failed (owner="
                        + action.owner + ", id=" + action.id + "):");
                t.printStackTrace();
            }
        }
        calledThisTick.clear();
    }

    public static void onWorldChange() {
        clearAll();
    }

    public static void clickNextTick(Object owner, int slot, int button, int mode,
                                     boolean allowDuplicates, int windowId, Consumer<ItemStack> callback) {
        schedule(owner, slot, allowDuplicates, () -> {
            ItemStack newStack = null;
            try {
                if (mc.thePlayer != null && mc.playerController != null) {
                    newStack = mc.playerController.windowClick(windowId, slot, button, mode, mc.thePlayer);
                }
            } catch (Throwable ignored) {
            }
            if (callback != null) {
                callback.accept(newStack);
            }
        });
    }

    public static void clickNextTick(Object owner, int slot, int button, int mode) {
        clickNextTick(owner, slot, button, mode, false,
                mc.thePlayer == null ? 0 : mc.thePlayer.openContainer.windowId, null);
    }

    public static void nextTick(Object owner, Runnable action) {
        schedule(owner, -1, true, action);
    }

    public static boolean isTicked(Object owner, int id) {
        return isScheduled(owner, id);
    }

    public static void clearTicked(Object owner) {
        clear(owner);
    }

    public static int countTicked(Object owner) {
        return size(owner);
    }

    public static boolean hasNoTicked(Object owner) {
        return isEmpty(owner);
    }

    public static List<Action> snapshot() {
        return new ArrayList<>(actions);
    }
}
