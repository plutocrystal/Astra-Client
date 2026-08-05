package dev.astra.value.impl;

import dev.astra.value.Value;

public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, boolean value) {
        super(name, value);
    }

    public boolean isEnabled() {
        return getValue();
    }

    public void toggle() {
        setValue(!getValue());
    }
}