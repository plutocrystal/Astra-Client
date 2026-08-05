package dev.astra.value.impl;

import dev.astra.value.Value;

import java.util.Arrays;
import java.util.List;

public class ModeValue extends Value<String> {
    private final List<String> modes;

    public ModeValue(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return getValue().equals(mode);
    }

    public void setMode(String mode) {
        if (modes.contains(mode)) {
            setValue(mode);
        }
    }
}