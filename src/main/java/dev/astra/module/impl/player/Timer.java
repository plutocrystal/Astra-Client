package dev.astra.module.impl.player;

import dev.astra.event.EventHandler;
import dev.astra.event.events.EventGameLoop;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;
import org.lwjgl.input.Keyboard;

public class Timer extends Module {

    public final NumberValue speed = new NumberValue("Speed", 2.0, 0.1, 10.0, 0.1);

    public Timer() {
        super("Timer", Keyboard.KEY_NONE, Category.PLAYER);
        addValues(speed);
    }

    @EventHandler
    public void onGameLoop(EventGameLoop event) {
        event.timerSpeed = speed.getValue().floatValue();
    }
}