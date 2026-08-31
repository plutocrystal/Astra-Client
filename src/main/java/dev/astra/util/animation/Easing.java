package dev.astra.util.animation;

import java.util.function.Function;

public enum Easing {
    Linear(x -> x),
    EaseInSine(x -> 1.0 - Math.cos(x * Math.PI / 2.0)),
    EaseOutSine(x -> Math.sin(x * Math.PI / 2.0)),
    EaseInOutSine(x -> -(Math.cos(Math.PI * x) - 1.0) / 2.0),
    EaseInQuad(x -> x * x),
    EaseOutQuad(x -> 1.0 - (1.0 - x) * (1.0 - x)),
    EaseInOutQuad(x -> x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0),
    EaseInCubic(x -> x * x * x),
    EaseOutCubic(x -> 1.0 - Math.pow(1.0 - x, 3.0)),
    EaseInOutCubic(x -> x < 0.5 ? 4.0 * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 3.0) / 2.0),
    EaseInQuart(x -> x * x * x * x),
    EaseOutQuart(x -> 1.0 - Math.pow(1.0 - x, 4.0)),
    EaseInOutQuart(x -> x < 0.5 ? 8.0 * x * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 4.0) / 2.0),
    EaseInQuint(x -> x * x * x * x * x),
    EaseOutQuint(x -> x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x)),
    EaseInOutQuint(x -> x < 0.5 ? 16.0 * x * x * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 5.0) / 2.0),
    EaseInExpo(x -> x == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * x - 10.0)),
    EaseOutExpo(x -> x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x)),
    EaseInOutExpo(x -> x == 0.0 ? 0.0 : (x == 1.0 ? 1.0 : (x < 0.5 ? Math.pow(2.0, 20.0 * x - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * x + 10.0)) / 2.0))),
    EaseInCirc(x -> 1.0 - Math.sqrt(1.0 - Math.pow(x, 2.0))),
    EaseOutCirc(x -> Math.sqrt(1.0 - Math.pow(x - 1.0, 2.0))),
    EaseInOutCirc(x -> x < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * x + 2.0, 2.0)) + 1.0) / 2.0),
    EaseInBack(x -> 2.70158 * x * x * x - 1.70158 * x * x),
    EaseOutBack(x -> 1.0 + 2.70158 * Math.pow(x - 1.0, 3.0) + 1.70158 * Math.pow(x - 1.0, 2.0)),
    EaseInOutBack(x -> x < 0.5 ? Math.pow(2.0 * x, 2.0) * (7.189819 * x - 2.5949095) / 2.0 : (Math.pow(2.0 * x - 2.0, 2.0) * (3.5949095 * (x * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0);

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    public Function<Double, Double> getFunction() {
        return function;
    }
}
