package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReprintDamageCalculatorTest {

    @Test
    void clampsLinearMinusOneToZero() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(1, true)));

        assertEquals(0.0f, result.factor());
        assertEquals(0, result.maxLevel());
    }

    @Test
    void clampsLinearMinusTwoToZero() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(2, true)));

        assertEquals(0.0f, result.factor());
    }

    @Test
    void clampsExactlyCancelledLinearPointsToZero() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(3, false),
                new ReprintDamageCalculator.Point(3, true)));

        assertEquals(0.0f, result.factor());
        assertEquals(3, result.maxLevel());
    }

    @Test
    void sumsPositiveLinearPoints() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(4, false),
                new ReprintDamageCalculator.Point(2, false),
                new ReprintDamageCalculator.Point(1, true)));

        assertEquals(5.0f, result.factor());
        assertEquals(4, result.maxLevel());
    }

    @Test
    void preservesOrdinaryExponentialCalculation() {
        var result = ReprintDamageCalculator.calculate(false, List.of(
                new ReprintDamageCalculator.Point(1, false),
                new ReprintDamageCalculator.Point(3, false)));

        assertEquals(5.0f, result.factor());
        assertEquals(3, result.maxLevel());
    }

    @Test
    void clampsOverCancelledExponentialPointsToZero() {
        var result = ReprintDamageCalculator.calculate(false, List.of(
                new ReprintDamageCalculator.Point(1, true)));

        assertEquals(0.0f, result.factor());
        assertEquals(0, result.maxLevel());
    }

    @Test
    void preservesHighLevelExponentialFallback() {
        var result = ReprintDamageCalculator.calculate(false, List.of(
                new ReprintDamageCalculator.Point(30, false),
                new ReprintDamageCalculator.Point(2, true)));

        assertEquals((float) Math.pow(2, 27), result.factor());
        assertEquals(30, result.maxLevel());
    }
}
