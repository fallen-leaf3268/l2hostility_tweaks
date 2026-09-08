package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.util.EntityConfigNbtData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftTraitSpawnIndexSourceTest {
    @Test
    void excludesInvalidNbtConfigsFromEveryIndexContext() {
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeConfig(EntityConfigNbtData.State.INVALID));
        assertTrue(MinecraftTraitSpawnIndexSource.shouldIncludeConfig(EntityConfigNbtData.State.NONE));
        assertTrue(MinecraftTraitSpawnIndexSource.shouldIncludeConfig(EntityConfigNbtData.State.VALID));
    }

    @Test
    void includesOnlyNonPlayerLivingEntitiesWithTraitCapability() {
        assertTrue(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(true, false, true));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(false, false, false));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(false, false, true));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(false, true, false));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(false, true, true));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(true, false, false));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(true, true, false));
        assertFalse(MinecraftTraitSpawnIndexSource.shouldIncludeEntity(true, true, true));
    }

    @Test
    void effectiveMaximumUsesEntityCapOrFallsBackToGlobalCap() {
        assertEquals(3000, MinecraftTraitSpawnIndexSource.effectiveMaxLevel(0, 3000));
        assertEquals(120, MinecraftTraitSpawnIndexSource.effectiveMaxLevel(120, 3000));
        assertEquals(3000, MinecraftTraitSpawnIndexSource.effectiveMaxLevel(5000, 3000));
    }

    @Test
    void capturePreservesRawMaximumTraitCountSentinel() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSource.java"));

        assertTrue(source.contains("config.maxTraitCount, presetTraitsOnly(config)"));
        assertFalse(source.contains("effectiveMaxTraitCount(config.maxTraitCount"));
    }

    @Test
    void captureEntitiesUsesTheL2HostilityTraitCapabilityGate() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSource.java"));

        assertTrue(source.contains("MobTraitCap.HOLDER.isProper(living)"));
        int playerGuard = source.indexOf("boolean player = living instanceof Player;");
        int capabilityGate = source.indexOf(
                "boolean traitCapabilityApplies = !player && MobTraitCap.HOLDER.isProper(living);");
        assertTrue(playerGuard >= 0);
        assertTrue(capabilityGate > playerGuard);
    }

    @Test
    void captureEvaluatesOverriddenRuntimeRulesAgainstEachLivingEntity() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSource.java"));

        assertTrue(source.contains("runtimeRejectedTraits(living)"));
        assertTrue(source.contains("if (!overridesRuntimeAllow(trait)) continue;"));
        assertTrue(source.contains("if (!baseAllowsRuntimeProbe(living, trait)) continue;"));
        assertTrue(source.contains("if (!trait.allow(living)) rejected.add(traitId);"));
        assertTrue(source.contains("Unable to evaluate runtime trait rule"));
    }
}
