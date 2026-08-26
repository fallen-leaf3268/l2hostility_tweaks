package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
