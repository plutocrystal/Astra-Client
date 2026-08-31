package dev.astra.module.impl.movement;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.misc.GameLoopEvent;
import dev.astra.event.events.packet.PacketSendEvent;
import dev.astra.event.events.player.PostUpdateEvent;
import dev.astra.event.events.player.PreUpdateEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.module.impl.player.Timer;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.GroupValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import java.util.ArrayList;
import java.util.List;

public class NoSlow extends Module {
    public static boolean ACTIVE = false;

    private static final int BLINK_QUEUE_LIMIT = 500;

    private final BooleanValue swordSprint = new BooleanValue("SwordSprint", false);

    private final BooleanValue foodSprint = new BooleanValue("FoodSprint", false);

    private final BooleanValue bowSprint = new BooleanValue("BowSprint", false);

    private final BooleanValue onlyWhileMove = new BooleanValue("Only while move", false);

    private final BooleanValue startSlow = new BooleanValue("Start slow", false);

    private final ModeValue switchMode = new ModeValue("Switch mode", "PreAttack", "PreAttack", "PostAttack", "PrePosition", "PostPosition");

    private final ModeValue slotMode = new ModeValue("Slot mode", "PreAttack", "PreAttack", "PostAttack", "PrePosition", "PostPosition");

    private final ModeValue sprintMode = new ModeValue("Sprint mode", "Legit", "Legit", "LegitMotion", "AllDirection", "AllDirectionMotion");

    private final ItemConfig swordConfig = new ItemConfig("Sword", swordSprint);

    private final ItemConfig bowConfig = new ItemConfig("Bow", bowSprint);

    private final ItemConfig restConfig = new ItemConfig("Rest", foodSprint);

    private int usingTicks = 0;

    private boolean lastUsed = false;

    private boolean sprintSuppressed = false;

    private boolean timerForced = false;

    private float targetTimerSpeed = 1.0f;

    private final List<Packet<?>> blinkQueue = new ArrayList<>();

    public NoSlow() {
        super("NoSlow", 0, Category.MOVEMENT);
        addValues(onlyWhileMove, startSlow, switchMode, slotMode, sprintMode,
                swordConfig.group, bowConfig.group, restConfig.group);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ACTIVE = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ACTIVE = false;
        sprintSuppressed = false;
        usingTicks = 0;
        lastUsed = false;
        timerForced = false;
        flushBlink();
    }

    private static class ItemConfig {
        final GroupValue group;
        final BooleanValue slowDown;
        final NumberValue slowDownForward;
        final NumberValue slowDownStrafe;
        final BooleanValue sprintToggle;
        final BooleanValue switchSwap;
        final BooleanValue slotResend;
        final BooleanValue onGroundSpoof;
        final BooleanValue toggleSpam;
        final BooleanValue inputSpoof;
        final BooleanValue inputForwardLegit;
        final NumberValue inputForward;
        final BooleanValue inputStrafeLegit;
        final NumberValue inputStrafe;
        final BooleanValue inputJumpLegit;
        final BooleanValue inputJump;
        final BooleanValue inputSneakLegit;
        final BooleanValue inputSneak;
        final BooleanValue bugFix;
        final BooleanValue timerEnabled;
        final NumberValue timerSpeedValue;
        final BooleanValue blinkEnabled;
        ItemConfig(String label, BooleanValue sprintToggle) {
            slowDown = new BooleanValue("Slowdown", false);
            slowDownForward = new NumberValue("SlowdownForward", 1.0, 0.2, 1.0, 0.01);
            slowDownStrafe = new NumberValue("SlowdownStrafe", 1.0, 0.2, 1.0, 0.01);
            this.sprintToggle = sprintToggle;
            switchSwap = new BooleanValue("Switch", false);
            slotResend = new BooleanValue("Slot", false);
            onGroundSpoof = new BooleanValue("OnGround", false);
            toggleSpam = new BooleanValue("Toggle", false);
            inputSpoof = new BooleanValue("Input", false);
            inputForwardLegit = new BooleanValue("InputForwardLegit", false);
            inputForward = new NumberValue("InputForward", 1.0, -1.0, 1.0, 0.01);
            inputStrafeLegit = new BooleanValue("InputStrafeLegit", false);
            inputStrafe = new NumberValue("InputStrafe", 0.0, -1.0, 1.0, 0.01);
            inputJumpLegit = new BooleanValue("InputJumpLegit", false);
            inputJump = new BooleanValue("InputJump", false);
            inputSneakLegit = new BooleanValue("InputSneakLegit", false);
            inputSneak = new BooleanValue("InputSneak", false);
            bugFix = new BooleanValue("Bug", false);
            timerEnabled = new BooleanValue("Timer", false);
            timerSpeedValue = new NumberValue("TimerValue", 0.2, 0.1, 2.0, 0.01);
            blinkEnabled = new BooleanValue("Blink", false);
            group = new GroupValue(label, slowDown, slowDownForward, slowDownStrafe, sprintToggle,
                    switchSwap, slotResend, onGroundSpoof, toggleSpam,
                    inputSpoof, inputForwardLegit, inputForward, inputStrafeLegit, inputStrafe,
                    inputJumpLegit, inputJump, inputSneakLegit, inputSneak,
                    bugFix, timerEnabled, timerSpeedValue, blinkEnabled);
        }
    }

    public static double getForwardSlowed(ItemStack heldItem) {
        return getCustomSlowed(heldItem, true);
    }

    public static double getStrafeSlowed(ItemStack heldItem) {
        return getCustomSlowed(heldItem, false);
    }

    private static double getCustomSlowed(ItemStack heldItem, boolean forward) {
        NoSlow module = activeModule();
        if (module == null || heldItem == null) return 0.2D;
        ItemConfig cfg = module.resolveConfig(heldItem);
        if (cfg == null) return 0.2D;
        if (module.startSlow.isEnabled() && module.usingTicks <= 1) return 0.2D;
        if (cfg.slowDown.isEnabled()) {
            return forward ? cfg.slowDownForward.getValue() : cfg.slowDownStrafe.getValue();
        }
        return 0.2D;
    }

    public static boolean shouldFakeNotSprinting() {
        NoSlow module = activeModule();
        if (module == null || mc.thePlayer == null || !mc.thePlayer.isUsingItem()) return false;
        return module.sprintMode.is("LegitMotion") || module.sprintMode.is("AllDirectionMotion");
    }

    public static boolean isOmniForced() {
        NoSlow module = activeModule();
        if (module == null || mc.thePlayer == null || !mc.thePlayer.isUsingItem()) return false;
        return module.sprintMode.is("AllDirection") || module.sprintMode.is("AllDirectionMotion");
    }

    public static double getSlowDownMultiplier(ItemStack heldItem) {
        return getCustomSlowed(heldItem, true);
    }

    public static boolean shouldStopSprint(ItemStack heldItem) {
        NoSlow module = activeModule();
        if (module == null || heldItem == null) return false;
        ItemConfig cfg = module.resolveConfig(heldItem);
        return cfg != null && cfg.sprintToggle.isEnabled();
    }

    private static NoSlow activeModule() {
        if (!ACTIVE) return null;
        return Main.moduleManager.getModuleByClass(NoSlow.class);
    }

    private ItemConfig resolveConfig(ItemStack heldItem) {
        if (heldItem == null) return null;
        Item item = heldItem.getItem();
        if (item instanceof ItemSword) return swordConfig;
        if (item instanceof ItemBow) return bowConfig;
        if (isRest(item)) return restConfig;
        return null;
    }

    private static boolean isRest(Item item) {
        return item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk;
    }

    private boolean noAction() {
        return onlyWhileMove.isEnabled() && !isMoving();
    }

    private static boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0.0f || mc.thePlayer.movementInput.moveStrafe != 0.0f;
    }

    private boolean canResumeSprint() {
        return mc.thePlayer.movementInput.moveForward >= 0.8F
                && !mc.thePlayer.isCollidedHorizontally
                && (mc.thePlayer.getFoodStats().getFoodLevel() > 6 || mc.thePlayer.capabilities.allowFlying);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        boolean using = mc.thePlayer.isUsingItem();
        if (using) {
            usingTicks++;
        } else {
            usingTicks = 0;
        }
        if (using && isOmniForced()) {
            mc.thePlayer.setSprinting(true);
        } else if (using && shouldStopSprint(mc.thePlayer.getHeldItem())) {
            if (mc.thePlayer.isSprinting()) {
                mc.thePlayer.setSprinting(false);
            }
            sprintSuppressed = true;
        } else if (!using && sprintSuppressed) {
            sprintSuppressed = false;
            if (canResumeSprint()) {
                mc.thePlayer.setSprinting(true);
            }
        }
        handleUseStateTransition(using);
        updateTimerState(using);
        ItemConfig cfg = resolveConfig(mc.thePlayer.getHeldItem());
        if (cfg == null || noAction()) return;
        if (cfg.onGroundSpoof.isEnabled()) {
            event.setOnGround(true);
        }
        if (switchMode.is("PreAttack")) doSwitch(cfg);
        if (slotMode.is("PreAttack")) doSlot(cfg);
        if (switchMode.is("PrePosition")) doSwitch(cfg);
        if (slotMode.is("PrePosition")) doSlot(cfg);
        if (cfg.toggleSpam.isEnabled() && using) {
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
        if (cfg.inputSpoof.isEnabled()) {
            float forward = cfg.inputForwardLegit.isEnabled()
                    ? mc.thePlayer.movementInput.moveForward : cfg.inputForward.getValue().floatValue();
            float strafe = cfg.inputStrafeLegit.isEnabled()
                    ? mc.thePlayer.movementInput.moveStrafe : cfg.inputStrafe.getValue().floatValue();
            boolean jump = cfg.inputJumpLegit.isEnabled()
                    ? mc.thePlayer.movementInput.jump : cfg.inputJump.isEnabled();
            boolean sneak = cfg.inputSneakLegit.isEnabled()
                    ? mc.thePlayer.movementInput.sneak : cfg.inputSneak.isEnabled();
            mc.thePlayer.sendQueue.addToSendQueue(new C0CPacketInput(strafe, forward, jump, sneak));
        }
    }

    @EventHandler
    public void onGameLoop(GameLoopEvent event) {
        if (timerForced && mc.thePlayer != null) {
            event.timerSpeed = targetTimerSpeed;
        }
    }

    private void updateTimerState(boolean using) {
        ItemConfig cfg = using ? resolveConfig(mc.thePlayer.getHeldItem()) : null;
        if (cfg != null && cfg.timerEnabled.isEnabled() && !noAction()
                && !isStandaloneTimerActive()) {
            targetTimerSpeed = cfg.timerSpeedValue.getValue().floatValue();
            timerForced = true;
        } else {
            timerForced = false;
        }
    }

    private static boolean isStandaloneTimerActive() {
        Timer timer = Main.moduleManager.getModuleByClass(Timer.class);
        return timer != null && timer.isToggle();
    }

    @EventHandler
    public void onPostUpdate(PostUpdateEvent event) {
        if (mc.thePlayer == null) return;
        ItemConfig cfg = resolveConfig(mc.thePlayer.getHeldItem());
        if (cfg == null || noAction()) return;
        if (switchMode.is("PostAttack")) doSwitch(cfg);
        if (slotMode.is("PostAttack")) doSlot(cfg);
        if (switchMode.is("PostPosition")) doSwitch(cfg);
        if (slotMode.is("PostPosition")) doSlot(cfg);
    }

    @EventHandler
    public void onSendPacket(PacketSendEvent event) {
        Packet<?> packet = event.getPacket();
        checkBugOption(packet, event);
        boolean holdable = packet instanceof C03PacketPlayer || packet instanceof C0BPacketEntityAction;
        if (!holdable || !isBlinking()) return;
        event.setCancelled(true);
        synchronized (blinkQueue) {
            blinkQueue.add(event.getPacket());
            if (blinkQueue.size() >= BLINK_QUEUE_LIMIT) {
                flushBlink();
            }
        }
    }

    private void checkBugOption(Packet<?> packet, PacketSendEvent event) {
        if (!(packet instanceof C08PacketPlayerBlockPlacement)) return;
        if (event.isCancelled() || mc.thePlayer == null) return;
        if (mc.thePlayer.isUsingItem()) return;
        ItemConfig cfg = resolveConfig(mc.thePlayer.getHeldItem());
        if (cfg == null || !cfg.bugFix.isEnabled()) return;
        event.setCancelled(true);
        mc.thePlayer.sendQueue.sendPacketNoEvent(event.getPacket());
        mc.thePlayer.sendQueue.sendPacketNoEvent(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
    }

    private void handleUseStateTransition(boolean using) {
        if (lastUsed && !using) {
            timerForced = false;
            flushBlink();
        }
        lastUsed = using;
    }

    private boolean isBlinking() {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem()) return false;
        ItemConfig cfg = resolveConfig(mc.thePlayer.getHeldItem());
        return cfg != null && cfg.blinkEnabled.isEnabled() && !noAction();
    }

    private void flushBlink() {
        List<Packet<?>> drained;
        synchronized (blinkQueue) {
            if (blinkQueue.isEmpty()) return;
            drained = new ArrayList<>(blinkQueue);
            blinkQueue.clear();
        }
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;
        for (Packet<?> packet : drained) {
            mc.thePlayer.sendQueue.sendPacketNoEvent(packet);
        }
    }

    private void doSwitch(ItemConfig cfg) {
        if (!cfg.switchSwap.isEnabled()) return;
        int slot = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot % 8 + 1));
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
    }

    private void doSlot(ItemConfig cfg) {
        if (!cfg.slotResend.isEnabled()) return;
        mc.thePlayer.sendQueue.addToSendQueue(
                new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }
}
