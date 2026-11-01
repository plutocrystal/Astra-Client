package dev.astra.module.impl.combat.killaura.block;

import dev.astra.module.impl.combat.killaura.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

public class AutoBlock {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura parent;

    private boolean fakeBlockState = false;

    public AutoBlock(KillAura parent) {
        this.parent = parent;
    }

    public void update(boolean hasTargets) {
        fakeBlockState = hasTargets && parent.autoBlockMode.is("Fake");
    }

    public boolean isFakeBlocking(ItemStack stack) {
        if (stack == null || stack.getItemUseAction() != EnumAction.BLOCK) return false;
        if (mc.thePlayer == null || mc.thePlayer.isUsingItem()) return false;
        return parent.isToggle() && parent.autoBlockMode.is("Fake") && fakeBlockState;
    }

    public void reset() {
        fakeBlockState = false;
    }
}
