package com.l2hostility_tweaks.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalDouble;

public final class MovementSpeedCapCalculator {

    private MovementSpeedCapCalculator() {
    }

    public static OptionalDouble calculateModifierAmount(double externalValue, double cap) {
        if (externalValue <= 0.0 || externalValue <= cap) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(cap / externalValue - 1.0);
    }

    public static double calculateExternalValue(double baseValue, List<Double> additions,
                                                List<Double> multiplyBase,
                                                List<Double> multiplyTotal) {
        double addedValue = baseValue;
        for (double amount : additions) {
            addedValue = applyAddition(addedValue, amount);
        }
        double value = addedValue;
        for (double amount : multiplyBase) {
            value = applyMultiplyBase(value, addedValue, amount);
        }
        for (double amount : multiplyTotal) {
            value = applyMultiplyTotal(value, amount);
        }
        return value;
    }

    public static double applyAddition(double value, double amount) {
        return value + amount;
    }

    public static double applyMultiplyBase(double value, double addedValue, double amount) {
        return value + addedValue * amount;
    }

    public static double applyMultiplyTotal(double value, double amount) {
        return value * (1.0 + amount);
    }

    public static String formatCap(double cap) {
        return BigDecimal.valueOf(cap).stripTrailingZeros().toPlainString();
    }
}
