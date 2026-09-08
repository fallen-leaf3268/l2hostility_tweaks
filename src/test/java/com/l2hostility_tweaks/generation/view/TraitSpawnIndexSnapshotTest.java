package com.l2hostility_tweaks.generation.view;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraitSpawnIndexSnapshotTest {
    @Test
    void copiesOverviewListsAndUsesEntityIdAsPageIdentity() {
        List<TraitSpawnIndexSnapshot.MobTraitOverview> mutable = new ArrayList<>();
        var overview = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("minecraft", "zombie"), List.of(), List.of(), List.of(), List.of(), List.of());
        mutable.add(overview);
        var snapshot = new TraitSpawnIndexSnapshot(7, mutable, 0);
        mutable.clear();

        assertEquals(1, snapshot.mobs().size());
        assertEquals(id("minecraft", "zombie"), snapshot.mobs().get(0).pageId());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.mobs().clear());
    }

    @Test
    void givesConditionalVariantsDistinctPageIdentity() {
        ResourceLocation entityId = id("goety", "apostle");
        var base = new TraitSpawnIndexSnapshot.MobTraitOverview(
                entityId, 0, List.of(), List.of(), List.of(), List.of(), List.of());
        var conditional = new TraitSpawnIndexSnapshot.MobTraitOverview(
                entityId, 1, List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals(entityId, base.pageId());
        assertEquals(id("goety", "apostle/condition/1"), conditional.pageId());
        assertThrows(IllegalArgumentException.class, () -> new TraitSpawnIndexSnapshot.MobTraitOverview(
                entityId, -1, List.of(), List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void copiesEveryNestedListDefensively() {
        List<TraitSpawnIndexSnapshot.EntityConfigView> configs = new ArrayList<>(List.of(entityConfig()));
        List<TraitSpawnIndexSnapshot.PresetTraitView> presets = new ArrayList<>(List.of(preset(0.5)));
        List<TraitSpawnIndexSnapshot.PoolTraitView> pool = new ArrayList<>(List.of(poolTrait()));
        List<TraitSpawnIndexSnapshot.BlockedTraitView> blocked = new ArrayList<>(List.of(blockedTrait()));
        List<TraitDynamicConstraint> overviewConstraints = new ArrayList<>(List.of(dynamicConstraint()));
        var overview = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("minecraft", "zombie"), configs, presets, pool, blocked, overviewConstraints);

        assertDefensiveCopy(configs, overview.configs());
        assertDefensiveCopy(presets, overview.presets());
        assertDefensiveCopy(pool, overview.pool());
        assertDefensiveCopy(blocked, overview.blocked());
        assertDefensiveCopy(overviewConstraints, overview.dynamicConstraints());

        List<TraitDynamicConstraint> presetConstraints = new ArrayList<>(List.of(dynamicConstraint()));
        var preset = new TraitSpawnIndexSnapshot.PresetTraitView(
                id("l2hostility", "adaptive"), id("l2hostility", "adaptive"),
                2, 3, false, 0.5, 200, id("l2hostility", "kill_10_traits"),
                id("example", "boss"), "{}", presetConstraints);
        assertDefensiveCopy(presetConstraints, preset.dynamicConstraints());

        List<TraitSpawnIndexSnapshot.BlockedContextView> contexts = new ArrayList<>(List.of(blockedContext()));
        var blockedWithMutableContexts = new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility", "adaptive"), id("l2hostility", "adaptive"), contexts);
        assertDefensiveCopy(contexts, blockedWithMutableContexts.contexts());

        List<TraitBlockReason> reasons = new ArrayList<>(List.of(TraitBlockReason.PRESET_ONLY));
        var context = new TraitSpawnIndexSnapshot.BlockedContextView(id("example", "source"), reasons);
        assertDefensiveCopy(reasons, context.reasons());

        List<String> arguments = new ArrayList<>(List.of("health", "100"));
        var constraint = new TraitDynamicConstraint("attribute", arguments);
        assertDefensiveCopy(arguments, constraint.arguments());
    }

    @Test
    void rejectsNullRequiredResourceIds() {
        assertThrows(NullPointerException.class, () -> new TraitSpawnIndexSnapshot.MobTraitOverview(
                null, List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> preset(null, id("l2hostility", "adaptive")));
        assertThrows(NullPointerException.class, () -> preset(id("l2hostility", "adaptive"), null));
        assertThrows(NullPointerException.class, () -> new TraitSpawnIndexSnapshot.PoolTraitView(
                null, id("l2hostility", "speedy"), 100, 50, 20, 5));
        assertThrows(NullPointerException.class, () -> new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility", "speedy"), null, 100, 50, 20, 5));
        assertThrows(NullPointerException.class, () -> new TraitSpawnIndexSnapshot.BlockedTraitView(
                null, id("l2hostility", "speedy"), List.of()));
        assertThrows(NullPointerException.class, () -> new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility", "speedy"), null, List.of()));
    }

    @Test
    void rejectsNonFiniteAndOutOfRangePresetChance() {
        for (double chance : List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                -0.001, 1.001)) {
            assertThrows(IllegalArgumentException.class, () -> preset(chance));
        }
    }

    @Test
    void rejectsNullDynamicConstraintTypeAndArguments() {
        assertThrows(NullPointerException.class, () -> new TraitDynamicConstraint(null, List.of()));
        assertThrows(NullPointerException.class, () -> new TraitDynamicConstraint("attribute", null));
        assertThrows(NullPointerException.class, () -> new TraitDynamicConstraint(
                "attribute", new ArrayList<>(java.util.Arrays.asList("ok", null))));
    }

    @Test
    void presetAndPoolUseDifferentProbabilitySemantics() {
        var preset = preset(0.5);
        var pool = poolTrait();

        assertEquals(0.5, preset.chance());
        assertEquals(100, pool.weight());
    }

    private static void assertDefensiveCopy(List<?> source, List<?> copy) {
        int expectedSize = source.size();
        source.clear();
        assertEquals(expectedSize, copy.size());
        assertThrows(UnsupportedOperationException.class, copy::clear);
    }

    private static TraitSpawnIndexSnapshot.EntityConfigView entityConfig() {
        return new TraitSpawnIndexSnapshot.EntityConfigView(
                null, "{}", 0, 1, 0.1, 1.0, 1.0, 1.0, 0.0, 1, 10, 3, false);
    }

    private static TraitSpawnIndexSnapshot.PresetTraitView preset(double chance) {
        return new TraitSpawnIndexSnapshot.PresetTraitView(
                id("l2hostility", "adaptive"), id("l2hostility", "adaptive"),
                2, 3, false, chance, 200, id("l2hostility", "kill_10_traits"),
                id("example", "boss"), "{}", List.of());
    }

    private static TraitSpawnIndexSnapshot.PresetTraitView preset(
            ResourceLocation traitId, ResourceLocation itemId) {
        return new TraitSpawnIndexSnapshot.PresetTraitView(
                traitId, itemId, 2, 3, false, 0.5, 200, id("l2hostility", "kill_10_traits"),
                id("example", "boss"), "{}", List.of());
    }

    private static TraitSpawnIndexSnapshot.PoolTraitView poolTrait() {
        return new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility", "speedy"), id("l2hostility", "speedy"), 100, 50, 20, 5);
    }

    private static TraitSpawnIndexSnapshot.BlockedTraitView blockedTrait() {
        return new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility", "adaptive"), id("l2hostility", "adaptive"), List.of(blockedContext()));
    }

    private static TraitSpawnIndexSnapshot.BlockedContextView blockedContext() {
        return new TraitSpawnIndexSnapshot.BlockedContextView(
                id("example", "source"), List.of(TraitBlockReason.PRESET_ONLY));
    }

    private static TraitDynamicConstraint dynamicConstraint() {
        return new TraitDynamicConstraint("attribute", List.of("health", "100"));
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
