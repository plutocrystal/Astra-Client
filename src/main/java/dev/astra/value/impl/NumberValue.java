package dev.astra.value.impl;

import dev.astra.value.Value;

public class NumberValue extends Value<Double> {
    private double min;
    private double max;
    private double increment;

    private NumberValue boundMax = null;
    private NumberValue boundMin = null;

    public NumberValue(String name, double default_value, double min, double max, double increment) {
        super(name, default_value);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public void setBoundMax(NumberValue boundMax) {
        this.boundMax = boundMax;
    }

    public void setBoundMin(NumberValue boundMin) {
        this.boundMin = boundMin;
    }

    @Override
    public void setValue(Double value) {
        if (boundMax != null && value > boundMax.getValue()) {
            value = boundMax.getValue();
        }
        if (boundMin != null && value < boundMin.getValue()) {
            value = boundMin.getValue();
        }
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        super.setValue(value);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }
}