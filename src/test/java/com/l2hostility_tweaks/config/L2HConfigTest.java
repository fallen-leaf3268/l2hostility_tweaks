package com.l2hostility_tweaks.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class L2HConfigTest {

    @Test
    void filtersInvalidExclusionIdsButKeepsUnregisteredIds() {
        var groups = L2HConfig.parseExclusionGroups(List.of(
                "first,l2hostility:gravity,Invalid Trait ID,addon:future_trait"));

        assertEquals(1, groups.size());
        assertEquals("first", groups.get(0).rule());
        assertEquals(List.of("l2hostility:gravity", "addon:future_trait"), groups.get(0).traitIds());
    }

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
