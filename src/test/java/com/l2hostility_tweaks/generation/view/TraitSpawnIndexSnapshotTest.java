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
    void presetAndPoolUseDifferentProbabilitySemantics() {
        var preset = new TraitSpawnIndexSnapshot.PresetTraitView(
                id("l2hostility", "adaptive"), id("l2hostility", "adaptive"),
                2, 3, false, 0.5, 200, id("l2hostility", "kill_10_traits"),
                id("example", "boss"), "{\"nbt\":{\"Health\":100}}", List.of());
        var pool = new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility", "speedy"), id("l2hostility", "speedy"), 100, 50, 20, 5);

        assertEquals(0.5, preset.chance());
        assertEquals(100, pool.weight());
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
