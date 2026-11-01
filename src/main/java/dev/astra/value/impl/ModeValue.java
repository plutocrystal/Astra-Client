package dev.astra.value.impl;

import dev.astra.value.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public class ModeValue extends Value<String> {
    private final List<String> modes = new ArrayList<>();

    private BiFunction<String, String, String> onChange;

    public ModeValue(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes.addAll(Arrays.asList(modes));
    }

    public void setOnChange(BiFunction<String, String, String> onChange) {
        this.onChange = onChange;
    }

    public List<String> getModes() {
        return modes;
    }

    public void setModes(List<String> newModes) {
        String previous = getValue();
        modes.clear();
        modes.addAll(newModes);
        if (!modes.contains(previous) && !modes.isEmpty()) {
            setValue(modes.get(0));
        }
    }

    public boolean is(String mode) {
        return getValue().equals(mode);
    }

    public void setMode(String mode) {
        if (modes.contains(mode)) {
            setValue(mode);
        }
    }

    @Override
    public void setValue(String value) {
        if (onChange != null) {
            value = onChange.apply(getValue(), value);
        }
        super.setValue(value);
    }
}
