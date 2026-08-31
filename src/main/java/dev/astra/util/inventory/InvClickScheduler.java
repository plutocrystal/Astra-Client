package dev.astra.util.inventory;

import dev.astra.util.RandomUtils;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import java.util.ArrayDeque;
import java.util.Deque;

public class InvClickScheduler {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static class Click {
        final int windowId;
        final int slot;
        final int button;
        final int mode;
        Click(int windowId, int slot, int button, int mode) {
            this.windowId = windowId;
            this.slot = slot;
            this.button = button;
            this.mode = mode;
        }
        String actionKey() {
            return mode + ":" + button;
        }
    }

    private final Deque<Click> queue = new ArrayDeque<>();

    private RangeSelectionNumberValue delayRange;

    private boolean instant;

    private boolean stopMove;

    private long nextClickTime = 0L;

    private long lastDelayMs = -1L;

    private int sameActionStreak = 0;

    private String lastActionKey = null;

    private int streakBreakAt = nextStreakBreak();

    private boolean freezing = false;

    public void configure(RangeSelectionNumberValue delayRange, boolean instant, boolean stopMove) {
        this.delayRange = delayRange;
        this.instant = instant;
        this.stopMove = stopMove;
    }

    public void enqueue(int windowId, int slot, int button, int mode) {
        queue.add(new Click(windowId, slot, button, mode));
    }

    public void enqueueDrop(int windowId, int slot) {
        if (RandomUtils.nextDouble() < 0.6D) {
            enqueue(windowId, slot, 1, 4);
        } else {
            enqueue(windowId, slot, 0, 0);
            enqueue(windowId, -999, 0, 0);
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean isBusy() {
        return !queue.isEmpty();
    }

    public boolean isFreezingMovement() {
        return freezing;
    }

    public long getNextClickTime() {
        return nextClickTime;
    }

    public void reset() {
        queue.clear();
        freezing = false;
        sameActionStreak = 0;
        lastActionKey = null;
        lastDelayMs = -1;
        nextClickTime = 0L;
        streakBreakAt = nextStreakBreak();
    }

    public void tick() {
        if (queue.isEmpty()) {
            freezing = false;
            return;
        }
        if (stopMove) {
            applyMovementGuard();
        }
        long now = System.currentTimeMillis();
        long extraPause = 0L;
        int budget = instant ? queue.size() : 1;
        if (!instant && now < nextClickTime) {
            return;
        }
        for (int i = 0; i < budget && !queue.isEmpty(); i++) {
            Click click = queue.poll();
            extraPause += trackStreak(click);
            if (mc.thePlayer != null && mc.playerController != null) {
                mc.playerController.windowClick(click.windowId, click.slot, click.button, click.mode, mc.thePlayer);
            }
        }
        if (!instant && !queue.isEmpty()) {
            nextClickTime = now + sampleDelay(delayRange) + extraPause;
        }
        if (instant || queue.isEmpty()) {
            freezing = false;
        }
    }

    private void applyMovementGuard() {
        freezing = true;
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.isSprinting()) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            mc.thePlayer.setSprinting(false);
        }
        mc.thePlayer.movementInput.moveForward = 0.0F;
        mc.thePlayer.movementInput.moveStrafe = 0.0F;
    }

    private long trackStreak(Click click) {
        String key = click.actionKey();
        if (key.equals(lastActionKey)) {
            sameActionStreak++;
        } else {
            sameActionStreak = 0;
        }
        lastActionKey = key;
        if (sameActionStreak >= streakBreakAt) {
            sameActionStreak = 0;
            streakBreakAt = nextStreakBreak();
            return (long) RandomUtils.nextDouble(400.0D, 900.0D);
        }
        return 0L;
    }

    public long sampleDelay(RangeSelectionNumberValue range) {
        double min = Math.max(0.0D, range.getMinValue());
        double max = Math.max(min, range.getMaxValue());
        double value;
        if (max - min < 1.0D) {
            double jitter = Math.max(12.0D, min * 0.15D);
            value = min + RandomUtils.nextDouble(-jitter, jitter);
        } else {
            double u1 = RandomUtils.nextDouble(min, max);
            double u2 = RandomUtils.nextDouble(min, max);
            value = (u1 + u2) / 2.0D;
        }
        long delay = Math.round(value);
        long minDelta = Math.max(8L, Math.round((max - min) * 0.15D));
        if (lastDelayMs >= 0L && Math.abs(delay - lastDelayMs) < minDelta) {
            delay += RandomUtils.nextDouble() < 0.5D ? -minDelta : minDelta;
        }
        long lower = Math.max(0L, Math.round(min) - minDelta);
        long upper = Math.round(max) + minDelta;
        delay = Math.max(lower, Math.min(upper, delay));
        lastDelayMs = delay;
        if (RandomUtils.nextDouble() < 0.08D) {
            delay += (long) RandomUtils.nextDouble(250.0D, 700.0D);
        }
        return delay;
    }

    private static int nextStreakBreak() {
        return RandomUtils.nextInt(4, 9);
    }
}
