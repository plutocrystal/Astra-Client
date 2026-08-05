package dev.astra.module;

import dev.astra.Main;
import dev.astra.value.Value;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public static final Minecraft mc = Minecraft.getMinecraft();
    
    private String name;
    private int keycode;
    private Category category;
    private boolean toggle;
    
    private final List<Value<?>> values = new ArrayList<>();
    
    public Module(String name, int keycode, Category category) {
        this.name = name;
        this.keycode = keycode;
        this.category = category;
        this.toggle = false;
    }
    
    public void addValues(Value<?>... values) {
        for (Value<?> value : values) {
            this.values.add(value);
        }
    }
    
    public List<Value<?>> getValues() {
        return values;
    }
    
    public String getName() { return name; }
    public int getKeycode() { return keycode; }
    public void setKeycode(int keycode) { this.keycode = keycode; }
    public Category getCategory() { return category; }
    public boolean isToggle() { return toggle; }
    
    public void toggle() {
        this.toggle = !this.toggle;
        if (this.toggle) {
            Main.eventBus.register(this);
            onEnable();
        } else {
            Main.eventBus.unregister(this);
            onDisable();
        }
    }
    
    public void onEnable() {}
    public void onDisable() {}
    
    public void setToggle(boolean toggle) { this.toggle = toggle; }
}