package dev.astra.module.impl.render;

import dev.astra.event.EventHandler;
import dev.astra.event.events.misc.TickEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ModeValue;
import dev.astra.value.impl.NumberValue;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

public class Ambience extends Module {
    public static Ambience INSTANCE;

    public NumberValue time = new NumberValue("Time", 6000.0, 0.0, 23999.0, 100.0);

    public ModeValue weather = new ModeValue("Weather", "Vanilla", "Vanilla", "Clear", "Rain", "Thunder", "Snow");

    private World lastWorld;

    private long savedTime;

    private boolean savedRaining;

    private boolean savedThundering;

    private boolean hasSaved;

    private float rainLevel;

    private float thunderLevel;

    public Ambience() {
        super("Ambience", Keyboard.KEY_NONE, Category.RENDER);
        addValues(time, weather);
        INSTANCE = this;
    }

    @Override
    public String getStatus() {
        return weather.is("Vanilla") ? "" : weather.getValue();
    }

    public static boolean forcePrecipitation() {
        return INSTANCE != null && INSTANCE.isToggle()
                && !INSTANCE.weather.is("Vanilla") && !INSTANCE.weather.is("Clear");
    }

    public static boolean forceSnow() {
        return INSTANCE != null && INSTANCE.isToggle() && INSTANCE.weather.is("Snow");
    }

    public static boolean isTimeForced() {
        return INSTANCE != null && INSTANCE.isToggle();
    }

    public static long getForcedTime() {
        return INSTANCE != null ? INSTANCE.time.getValue().longValue() : 0L;
    }

    public static boolean isWeatherForced() {
        return INSTANCE != null && INSTANCE.isToggle() && !INSTANCE.weather.is("Vanilla");
    }

    @Override
    public void onEnable() {
        lastWorld = null;
        hasSaved = false;
    }

    @Override
    public void onDisable() {
        restore();
        lastWorld = null;
        hasSaved = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        World world = mc.theWorld;
        if (world != lastWorld) {
            lastWorld = world;
            hasSaved = false;
            rainLevel = world.getRainStrength(1.0F);
            thunderLevel = 0.0F;
        }
        if (!hasSaved) {
            savedTime = world.getWorldTime();
            savedRaining = world.getWorldInfo().isRaining();
            savedThundering = world.getWorldInfo().isThundering();
            hasSaved = true;
        }
        world.setWorldTime(time.getValue().longValue());
        if (!weather.is("Vanilla")) {
            float targetRain = weather.is("Rain") || weather.is("Thunder") || weather.is("Snow") ? 1.0F : 0.0F;
            float targetThunder = weather.is("Thunder") ? 1.0F : 0.0F;
            rainLevel = approach(rainLevel, targetRain);
            thunderLevel = approach(thunderLevel, targetThunder);
            world.setRainStrength(rainLevel);
            world.setThunderStrength(thunderLevel);
            world.getWorldInfo().setRaining(targetRain > 0.5F);
            world.getWorldInfo().setThundering(targetThunder > 0.5F);
        }
    }

    private float approach(float current, float target) {
        if (current < target) {
            current += 0.01F;
            if (current > target) {
                current = target;
            }
        } else if (current > target) {
            current -= 0.01F;
            if (current < target) {
                current = target;
            }
        }
        return current;
    }

    private void restore() {
        if (!hasSaved || mc.theWorld == null) return;
        mc.theWorld.setWorldTime(savedTime);
        mc.theWorld.getWorldInfo().setRaining(savedRaining);
        mc.theWorld.getWorldInfo().setThundering(savedThundering);
        mc.theWorld.setRainStrength(savedRaining ? 1.0F : 0.0F);
        mc.theWorld.setThunderStrength(savedThundering ? 1.0F : 0.0F);
    }
}
