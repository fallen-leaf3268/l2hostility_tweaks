package com.l2hostility_tweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class L2HConfigTest {

    @Test
    void killerAuraIntervalsRequirePositiveIntegers() {
        assertTrue(L2HConfig.isPositiveInteger(1));
        assertFalse(L2HConfig.isPositiveInteger(0));
        assertFalse(L2HConfig.isPositiveInteger(-1));
        assertFalse(L2HConfig.isPositiveInteger("1"));
    }

    @Test
    void killerAuraIntervalNeverFallsBelowOneTick() {
        assertEquals(20, L2HConfig.sanitizeKillerAuraInterval(20));
        assertEquals(1, L2HConfig.sanitizeKillerAuraInterval(0));
        assertEquals(1, L2HConfig.sanitizeKillerAuraInterval(-20));
    }
}
