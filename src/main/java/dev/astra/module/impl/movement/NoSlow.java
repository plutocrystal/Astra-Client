package dev.astra.module.impl.movement;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import dev.astra.value.impl.MultiBooleanValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class NoSlow extends Module {
    public static boolean ACTIVE = false;

    private final NumberValue swordSlowDown = new NumberValue("SwordSlowDown", 1.0, 0.0, 1.0, 0.01);
    private final MultiBooleanValue swordOptions = new MultiBooleanValue("SwordOptions").add("Sprint", false);
    private final NumberValue foodSlowDown = new NumberValue("FoodSlowDown", 1.0, 0.0, 1.0, 0.01);
    private final MultiBooleanValue foodOptions = new MultiBooleanValue("FoodOptions").add("Sprint", false);
    private final NumberValue bowSlowDown = new NumberValue("BowSlowDown", 1.0, 0.0, 1.0, 0.01);
    private final MultiBooleanValue bowOptions = new MultiBooleanValue("BowOptions").add("Sprint", false);

    private boolean sentStopSprint = false;

    public NoSlow() {
        super("NoSlow", 0, Category.MOVEMENT);
        addValues(swordSlowDown, swordOptions, foodSlowDown, foodOptions, bowSlowDown, bowOptions);
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
        restoreSprint();
    }

    public static double getSlowDownMultiplier(ItemStack heldItem) {
        if (!ACTIVE || heldItem == null) return 0.2D;

        NoSlow module = Main.moduleManager.getModuleByClass(NoSlow.class);
        if (module == null) return 0.2D;

        Item item = heldItem.getItem();
        if (item instanceof ItemSword) return module.swordSlowDown.getValue();
        if (item instanceof ItemFood || item instanceof ItemPotion) return module.foodSlowDown.getValue();
        if (item instanceof ItemBow) return module.bowSlowDown.getValue();
        return 0.2D;
    }

    public static boolean shouldStopSprint(ItemStack heldItem) {
        if (!ACTIVE || heldItem == null) return false;

        NoSlow module = Main.moduleManager.getModuleByClass(NoSlow.class);
        if (module == null) return false;

        Item item = heldItem.getItem();
        if (item instanceof ItemSword) return module.swordOptions.getValue("Sprint");
        if (item instanceof ItemFood || item instanceof ItemPotion) return module.foodOptions.getValue("Sprint");
        if (item instanceof ItemBow) return module.bowOptions.getValue("Sprint");
        return false;
    }

    private void restoreSprint() {
        if (sentStopSprint && mc.thePlayer != null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            sentStopSprint = false;
        }
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null) return;

        boolean usingItem = mc.thePlayer.isUsingItem();

        if (usingItem && shouldStopSprint(mc.thePlayer.getHeldItem())) {
            if (mc.thePlayer.isSprinting() && !sentStopSprint) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.setSprinting(false);
                sentStopSprint = true;
            }
        }

        if (!usingItem) {
            restoreSprint();
        }
    }
}
