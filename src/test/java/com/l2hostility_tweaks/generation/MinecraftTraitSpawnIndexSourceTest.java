package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.util.EntityConfigNbtData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftTraitSpawnIndexSourceTest {
    @Test
    void excludesInvalidNbtConfigsFromEveryIndexContext() {
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeConfig(EntityConfigNbtData.State.INVALID));
        assertTrue(MinecraftTraitSpawnIndexSource.shouldIncludeConfig(EntityConfigNbtData.State.NONE));
        assertTrue(MinecraftTraitSpawnIndexSource.shouldIncludeConfig(EntityConfigNbtData.State.VALID));
    }
}

