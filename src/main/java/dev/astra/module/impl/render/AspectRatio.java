package dev.astra.module.impl.render;

import dev.astra.module.Category;
import dev.astra.module.Module;
import dev.astra.value.impl.NumberValue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class AspectRatio extends Module {
    private static final DecimalFormat df =
            new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

    public final NumberValue ratio = new NumberValue("Ratio", 1.78, 0.0, 5.0, 0.01);

    public AspectRatio() {
        super("AspectRatio", 0, Category.RENDER);
        addValues(ratio);
    }

    public float getAspect(float vanilla) {
        float value = ratio.getValue().floatValue();
        return value > 0.0F ? value : vanilla;
    }

    @Override
    public String getStatus() {
        return df.format(ratio.getValue()) + "x";
    }
}