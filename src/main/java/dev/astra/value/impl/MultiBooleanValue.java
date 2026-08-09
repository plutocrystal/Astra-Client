package dev.astra.value.impl;

import dev.astra.value.Value;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiBooleanValue extends Value<Map<String, Boolean>> {

    public MultiBooleanValue(String name) {
        super(name, new LinkedHashMap<>());
    }

    public MultiBooleanValue add(String key, boolean defaultValue) {
        getValue().put(key, defaultValue);
        return this;
    }

    public boolean getValue(String key) {
        return getValue().getOrDefault(key, false);
    }

    public void setValue(String key, boolean value) {
        getValue().put(key, value);
    }

    public void toggle(String key) {
        setValue(key, !getValue(key));
    }
}