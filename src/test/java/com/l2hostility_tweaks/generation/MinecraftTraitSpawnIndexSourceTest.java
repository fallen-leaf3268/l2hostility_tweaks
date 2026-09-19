package com.l2hostility_tweaks.generation;

import com.google.gson.JsonParser;
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
    void jeiDisplayNameIsAcceptedOnlyForValidNbtRules() {
        var raw = JsonParser.parseString("{\"jeiDisplayName\":\"  使徒（天启形态）  \"}")
                .getAsJsonObject();
        var invalidType = JsonParser.parseString("{\"jeiDisplayName\":12}").getAsJsonObject();
        var blank = JsonParser.parseString("{\"jeiDisplayName\":\"   \"}").getAsJsonObject();

        assertEquals("使徒（天启形态）", MinecraftTraitSpawnIndexSource.readJeiDisplayName(
                EntityConfigNbtData.State.VALID, raw));
        assertEquals("", MinecraftTraitSpawnIndexSource.readJeiDisplayName(
                EntityConfigNbtData.State.NONE, raw));
        assertEquals("", MinecraftTraitSpawnIndexSource.readJeiDisplayName(
                EntityConfigNbtData.State.VALID, invalidType));
        assertEquals("", MinecraftTraitSpawnIndexSource.readJeiDisplayName(
                EntityConfigNbtData.State.VALID, blank));
    }

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
    void clampsConfiguredProbabilitiesToTheirEffectiveRuntimeRange() {
        assertEquals(0.0, MinecraftTraitSpawnIndexSource.effectiveProbability(-1.0));
        assertEquals(0.0, MinecraftTraitSpawnIndexSource.effectiveProbability(0.0));
        assertEquals(0.25, MinecraftTraitSpawnIndexSource.effectiveProbability(0.25));
        assertEquals(1.0, MinecraftTraitSpawnIndexSource.effectiveProbability(1.0));
        assertEquals(1.0, MinecraftTraitSpawnIndexSource.effectiveProbability(100.0));
        assertEquals(Double.NaN, MinecraftTraitSpawnIndexSource.effectiveProbability(Double.NaN));
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
    void captureMirrorsLastWinsBaseConfigsAndDeduplicatesEntitySelectors() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSource.java"));

        assertTrue(source.contains("Map<ResourceLocation, ConfigInput> base"));
        assertTrue(source.contains("new LinkedHashSet<>(config.entities)"));
        assertTrue(source.contains("base.put(entityId, input)"));
        assertFalse(source.contains("base.computeIfAbsent"));
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

    @Test
    void captureUsesTheParsedEffectiveEntityItemPools() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSource.java"));

        assertTrue(source.contains("MobEquipmentIndex.capture(config, sourceId)"));
        assertTrue(source.contains("view, blacklist, presets, equipment"));
    }
}
