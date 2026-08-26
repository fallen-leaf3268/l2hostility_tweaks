package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetTraitLevelMixinTest {

    @Test
    void onlyPositiveRawLevelsAreActive() {
        assertTrue(GetTraitLevelMixin.l2fix$isActiveLevel(1));
        assertFalse(GetTraitLevelMixin.l2fix$isActiveLevel(0));
        assertFalse(GetTraitLevelMixin.l2fix$isActiveLevel(-1));
    }

    @Test
    void iteratesActiveTraitsWithoutCopyingTheTraitMap() throws Exception {
        var method = Arrays.stream(TraitSealFilterMixin.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("l2fix$forEachActive"))
                .findFirst().orElse(null);
        assertNotNull(method);
        method.setAccessible(true);

        var traits = new LinkedHashMap<String, Integer>();
        traits.put("active", 2);
        traits.put("sealed", -3);
        traits.put("absent", 0);
        var visited = new ArrayList<String>();
        BiConsumer<String, Integer> consumer = (trait, level) -> visited.add(trait + ":" + level);

        method.invoke(null, traits, consumer);

        assertEquals(List.of("active:2"), visited);
        assertEquals(List.of("active", "sealed", "absent"), new ArrayList<>(traits.keySet()));
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitSealFilterMixin.java"));
        assertFalse(source.contains("new LinkedHashMap<"));
        assertTrue(source.contains("Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V"));
    }

    @Test
    void clearsCounterStrikeTargetWhenItBecomesInvalid() {
        var strikeId = UUID.randomUUID();

        assertNull(CounterStrikeTraitMixin.l2fix$clearInvalidTarget(strikeId, false));
        assertSame(strikeId, CounterStrikeTraitMixin.l2fix$clearInvalidTarget(strikeId, true));
    }

    @Test
    void validatesCounterStrikeTargetBeforeEarlyTickReturns() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/CounterStrikeTraitMixin.java"));
        int validation = source.indexOf("data.strikeId = l2fix$clearInvalidTarget");
        int cooldown = source.indexOf("if (data.cooldown > 0)");
        int onGround = source.indexOf("if (!le.onGround())");

        assertTrue(validation >= 0);
        assertTrue(validation < cooldown);
        assertTrue(validation < onGround);
    }
}
