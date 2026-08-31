package dev.astra.module.impl.misc;

import dev.astra.event.EventHandler;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.BooleanValue;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Fixes extends Module {
    public static Fixes INSTANCE;

    public final BooleanValue mouseDelayFix = new BooleanValue("MouseDelayFix", true);

    public final BooleanValue hitDelayFix = new BooleanValue("HitDelayFix", false);

    public final BooleanValue allowTPInBlock = new BooleanValue("AllowTPInBlock", true);

    public final BooleanValue allowGuiInPortal = new BooleanValue("AllowGuiInPortal", true);

    public Fixes() {
        super("Fixes", Keyboard.KEY_NONE, Category.MISC);
        INSTANCE = this;
        addValues(mouseDelayFix, hitDelayFix, allowTPInBlock, allowGuiInPortal);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!allowTPInBlock.isEnabled() && isHeadInsideBlock()) {
            mc.gameSettings.thirdPersonView = 0;
        }
    }

    private boolean isHeadInsideBlock() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, 0, 0);

        for (int i = 0; i < 8; ++i) {
            int x = MathHelper.floor_double(mc.thePlayer.posX + (double) (((float) ((i >> 1) % 2) - 0.5F) * mc.thePlayer.width * 0.8F));
            int y = MathHelper.floor_double(mc.thePlayer.posY + (double) (((float) ((i >> 0) % 2) - 0.5F) * 0.1F) + (double) mc.thePlayer.getEyeHeight());
            int z = MathHelper.floor_double(mc.thePlayer.posZ + (double) (((float) ((i >> 2) % 2) - 0.5F) * mc.thePlayer.width * 0.8F));

            if ((pos.getX() != x || pos.getY() != y || pos.getZ() != z)
                    && mc.theWorld.getBlockState(pos.set(x, y, z)).getBlock().isVisuallyOpaque()) {
                return true;
            }
        }

        return false;
    }
}
