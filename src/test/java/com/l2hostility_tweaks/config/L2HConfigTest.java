package com.l2hostility_tweaks.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class L2HConfigTest {

    @Test
    void invalidatesEveryParsedCommonConfigCache() throws Exception {
        List<String> cacheNames = List.of(
                "parsedLevelThresholds",
                "parsedPerTraitThresholds",
                "parsedLegendaryThresholds",
                "parsedExtraLegendaryIds",
                "parsedExclusionGroups",
                "parsedSealDurationArray",
                "parsedPlayerTraitOverrides");

        for (String cacheName : cacheNames) {
            Field field = L2HConfig.class.getDeclaredField(cacheName);
            field.setAccessible(true);
            Object sentinel = Map.class.isAssignableFrom(field.getType())
                    ? new LinkedHashMap<>()
                    : Set.class.isAssignableFrom(field.getType())
                    ? new LinkedHashSet<>()
                    : new ArrayList<>();
            field.set(null, sentinel);
        }

        L2HConfig.invalidateCaches();

        for (String cacheName : cacheNames) {
            Field field = L2HConfig.class.getDeclaredField(cacheName);
            field.setAccessible(true);
            assertNull(field.get(null), cacheName);
        }
    }

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
