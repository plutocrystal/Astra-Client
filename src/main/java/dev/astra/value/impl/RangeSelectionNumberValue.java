package dev.astra.value.impl;

import dev.astra.value.Value;

public class RangeSelectionNumberValue extends Value<double[]> {
    private final double min;
    private final double max;
    private final double increment;

    public RangeSelectionNumberValue(String name, double defaultMin, double defaultMax, double min, double max, double increment) {
        super(name, new double[]{defaultMin, defaultMax});
        this.min = min;
        this.max = max;
        this.increment = increment;
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

    public double getMinValue() {
        return getValue()[0];
    }

    public double getMaxValue() {
        return getValue()[1];
    }

    public void setValues(double minVal, double maxVal) {
        if (minVal < min) minVal = min;
        if (maxVal > max) maxVal = max;
        if (minVal > maxVal) minVal = maxVal;
        if (maxVal < minVal) maxVal = minVal;
        
        if (increment > 0) {
            minVal = Math.round(minVal / increment) * increment;
            maxVal = Math.round(maxVal / increment) * increment;
        }
        
        getValue()[0] = minVal;
        getValue()[1] = maxVal;
    }
}