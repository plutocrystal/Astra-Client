package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.input.Keyboard;

public class NoHurtTime extends Module {

    public NoHurtTime() {
        super("NoHurtTime", Keyboard.KEY_NONE, Category.RENDER);
    }
}