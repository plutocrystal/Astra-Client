package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.RangeSelectionNumberValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class AutoArmor extends Module {

    public final ModeValue mode = new ModeValue("Mode", "OpenInv", "OpenInv", "SpoofInv", "Basic");
    public final BooleanValue noMove = new BooleanValue("NoMove", false);
    public final BooleanValue interactionCheck = new BooleanValue("InteractCheck", false);
    public final RangeSelectionNumberValue delay = new RangeSelectionNumberValue("Delay", 50.0, 150.0, 0.0, 1000.0, 1.0);

    private long lastAction = 0L;
    private boolean serverInvOpen = false;
    private boolean blockInv = false;

    public AutoArmor() {
        super("AutoArmor", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(mode, noMove, interactionCheck, delay);
    }

    @Override
    public void onDisable() {
        closeInv();
        super.onDisable();
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        boolean isMoving = mc.thePlayer.moveStrafing != 0 || mc.thePlayer.moveForward != 0;
        boolean invCleaner = mc.currentScreen instanceof GuiInventory || (!mode.is("OpenInv") && (mc.currentScreen == null || mc.currentScreen instanceof GuiInventory) && (!noMove.getValue() || (noMove.getValue() && !isMoving)));

        if (invCleaner && !blockInv) {
            long min = (long) delay.getMinValue();
            long max = (long) delay.getMaxValue();
            if (max < min) max = min;
            long currentDelay = ThreadLocalRandom.current().nextLong(min, max + 1);
            
            if (System.currentTimeMillis() - lastAction >= currentDelay) {
                openInv();
                if (handleAutoArmor()) {
                    lastAction = System.currentTimeMillis();
                    return;
                }
            }
        } else {
            closeInv();
        }
    }

    private boolean handleAutoArmor() {
        for (int armorType = 0; armorType < 4; armorType++) {
            int armorSlot = 5 + armorType;
            ItemStack currentArmor = mc.thePlayer.openContainer.getSlot(armorSlot).getStack();
            int bestInvSlot = -1;
            double bestScore = -1;

            for (int invSlot = 9; invSlot < 45; invSlot++) {
                ItemStack stack = mc.thePlayer.openContainer.getSlot(invSlot).getStack();
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    ItemArmor armor = (ItemArmor) stack.getItem();
                    if (armor.armorType == armorType) {
                        double score = getDamageReduceAmount(stack);
                        if (score > bestScore) {
                            bestScore = score;
                            bestInvSlot = invSlot;
                        }
                    }
                }
            }

            double currentScore = currentArmor != null ? getDamageReduceAmount(currentArmor) : -1;
            if (bestInvSlot != -1 && bestScore > currentScore) {
                if (currentArmor != null) {
                    mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, armorSlot, 0, 1, mc.thePlayer);
                } else {
                    mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, bestInvSlot, 0, 1, mc.thePlayer);
                }
                return true;
            }
        }
        return false;
    }

    private void closeInv() {
        if (serverInvOpen && mode.is("SpoofInv") && mc.currentScreen == null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            serverInvOpen = false;
        }
    }

    private void openInv() {
        if (mode.is("SpoofInv") && !serverInvOpen && mc.currentScreen == null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            serverInvOpen = true;
        }
    }

    private double getDamageReduceAmount(ItemStack itemStack) {
        double damageReduceAmount = 0.0;
        if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
            damageReduceAmount += ((ItemArmor) itemStack.getItem()).damageReduceAmount + (6 + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack)) / 3.0f;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, itemStack) / 11.0;
            damageReduceAmount += EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, itemStack) / 11.0;
            damageReduceAmount -= itemStack.getItemDamage() / 10000.0;
        }
        return damageReduceAmount;
    }
}