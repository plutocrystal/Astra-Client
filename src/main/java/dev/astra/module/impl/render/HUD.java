package dev.astra.module.impl.render;

import dev.astra.Main;
import dev.astra.event.EventHandler;
import dev.astra.event.events.Render2DEvent;
import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.ColorValue;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HUD extends Module {

    public ColorValue color = new ColorValue("Color", 0xFFA064FF);

    public HUD() {
        super("HUD", Keyboard.KEY_NONE, Category.RENDER);
        getValues().add(color);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String buildText = "Build-" + dateStr;
        
        int buildX = screenWidth - mc.fontRendererObj.getStringWidth(buildText) - 2;
        int buildY = screenHeight - 10;
        mc.fontRendererObj.drawStringWithShadow(buildText, buildX, buildY, color.getValue());

        List<Module> enabledModules = new ArrayList<>();
        for (Module mod : Main.moduleManager.getModules()) {
            if (mod.isToggle() && !(mod instanceof HUD) && !(mod instanceof ClickGui)) {
                enabledModules.add(mod);
            }
        }

        enabledModules.sort(Comparator.comparingInt(m -> -mc.fontRendererObj.getStringWidth(m.getName())));

        int y = 2;
        for (Module mod : enabledModules) {
            String name = mod.getName();
            int x = screenWidth - mc.fontRendererObj.getStringWidth(name) - 2;
            mc.fontRendererObj.drawStringWithShadow(name, x, y, color.getValue());
            y += 10;
        }
    }
}