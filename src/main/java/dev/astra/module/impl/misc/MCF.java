package dev.astra.module.impl.misc;

import dev.astra.util.ChatUtil;
import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPreUpdate;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.utils.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class MCF extends Module {

    private boolean wasPressed = false;

    public MCF() {
        super("MCF", Keyboard.KEY_NONE, Category.MISC);
    }

    @EventHandler
    public void onPreUpdate(EventPreUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (Mouse.isButtonDown(2)) {
            if (!wasPressed) {
                wasPressed = true;
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    if (entity instanceof EntityPlayer) {
                        String name = entity.getName();
                        boolean isFriend = FriendManager.toggleFriend(name);
                        if (isFriend) {
                            ChatUtil.sendMessage("§a" + name + " has been added to your friends.");
                        } else {
                            ChatUtil.sendMessage("§c" + name + " has been removed from your friends.");
                        }
                    }
                }
            }
        } else {
            wasPressed = false;
        }
    }
}