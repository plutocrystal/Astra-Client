package dev.astra.module.impl.misc;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventPacketReceive;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.ChatUtil;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import org.lwjgl.input.Keyboard;

public class FlagDetector extends Module {

    public static int counter = 0;

    public FlagDetector() {
        super("FlagDetector", Keyboard.KEY_NONE, Category.MISC);
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            counter++;
            ChatUtil.sendMessage("§cFlag Detected: §7" + counter);
        }
    }
}