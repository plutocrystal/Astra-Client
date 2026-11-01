package dev.astra.module.impl.render;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.render.Render2DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.util.animation.Animation;
import dev.astra.util.animation.Easing;
import dev.astra.value.impl.ColorValue;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HUD extends Module {
    public ColorValue color = new ColorValue("Color", 0xFFA064FF);

    public ColorValue statusColor = new ColorValue("StatusColor", 0xFFFFFFFF);

    private static final Easing ANIMATION_EASING = Easing.EaseOutQuad;

    private static final long ANIMATION_DURATION = 250L;

    private final Map<Module, Animation> animations = new HashMap<>();

    private float displayAlpha = 1.0f;

    private long lastFrameTime = System.currentTimeMillis();

    public HUD() {
        super("HUD", Keyboard.KEY_NONE, Category.RENDER);
        getValues().add(color);
        getValues().add(statusColor);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastFrameTime) / 1000.0f, 0.1f);
        lastFrameTime = now;
        float targetAlpha = color.getAlpha() / 255.0f;
        displayAlpha += (targetAlpha - displayAlpha) * (1.0f - (float) Math.exp(-12.0f * dt));
        if (Math.abs(targetAlpha - displayAlpha) < 0.002f) {
            displayAlpha = targetAlpha;
        }
        int a = Math.round(displayAlpha * 255.0f);
        if (a <= 0) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        Set<Module> enabledSet = new HashSet<>();
        List<Module> drawList = new ArrayList<>();
        for (Module mod : Main.moduleManager.getModules()) {
            if (mod.isToggle() && !mod.isHidden() && !(mod instanceof HUD) && !(mod instanceof ClickGui)) {
                enabledSet.add(mod);
                drawList.add(mod);
            }
        }
        for (Module mod : animations.keySet()) {
            if (!enabledSet.contains(mod)) {
                drawList.add(mod);
            }
        }
        if (drawList.isEmpty()) return;
        Map<Module, String> displayNames = new HashMap<>();
        Map<Module, String> displayStatuses = new HashMap<>();
        for (Module mod : drawList) {
            String status = mod.getStatus();
            displayNames.put(mod, mod.getName());
            displayStatuses.put(mod, status == null ? "" : status);
        }
        drawList.sort(Comparator.comparingInt(m -> {
            String name = displayNames.get(m);
            String status = displayStatuses.get(m);
            return -mc.fontRendererObj.getStringWidth(status.isEmpty() ? name : name + " " + status);
        }));
        int y = 2;
        for (Module mod : drawList) {
            Animation anim = animations.computeIfAbsent(mod, m -> new Animation(ANIMATION_EASING, ANIMATION_DURATION));
            boolean enabled = enabledSet.contains(mod);
            anim.run(enabled ? 1.0 : 0.0);
            if (!enabled && anim.isFinished()) {
                animations.remove(mod);
                continue;
            }
            double progress = anim.getValue();
            String name = displayNames.get(mod);
            String status = displayStatuses.get(mod);
            int textWidth = mc.fontRendererObj.getStringWidth(status.isEmpty() ? name : name + " " + status);
            float x = screenWidth - textWidth - 2 + (float) ((1.0 - progress) * (textWidth + 2));
            int entryAlpha = (int) (a * progress);
            if (entryAlpha > 0) {
                mc.fontRendererObj.drawStringWithShadow(name, x, y, (entryAlpha << 24) | (color.getValue() & 0x00FFFFFF));
                if (!status.isEmpty()) {
                    float statusX = x + mc.fontRendererObj.getStringWidth(name + " ");
                    mc.fontRendererObj.drawStringWithShadow(status, statusX, y,
                            (entryAlpha << 24) | (statusColor.getValue() & 0x00FFFFFF));
                }
            }
            y += 10;
        }
    }
}
