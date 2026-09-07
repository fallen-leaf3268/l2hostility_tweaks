package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TraitSpawnIndexCodecTest {

    @Test
    void roundTripsCompleteSnapshot() {
        TraitSpawnIndexSnapshot expected = completeSnapshot(9);

        assertEquals(expected, TraitSpawnIndexCodec.decode(TraitSpawnIndexCodec.encode(expected)));
    }

    @Test
    void rejectsOversizedMobListBeforeAllocation() {
        CompoundTag root = new CompoundTag();
        root.putLong("revision", 1);
        root.putInt("mobCount", TraitSpawnIndexCodec.MAX_MOBS + 1);

        assertThrows(IllegalArgumentException.class, () -> TraitSpawnIndexCodec.decode(root));
    }

    @Test
    void rejectsInvalidProbabilityAndResourceId() {
        assertThrows(IllegalArgumentException.class,
                () -> TraitSpawnIndexCodec.validateProbability(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> TraitSpawnIndexCodec.parseId("bad id"));
    }

    public static TraitSpawnIndexSnapshot completeSnapshot(long revision) {
        TraitDynamicConstraint constraint = new TraitDynamicConstraint("attribute", List.of("health", "100"));
        TraitSpawnIndexSnapshot.EntityConfigView config = new TraitSpawnIndexSnapshot.EntityConfigView(
                id("example:config"), "{\"nbt\":{\"Health\":100}}", 1, 2, 0.5, 1.25,
                0.75, 0.5, 0.1, 3, 50, 4, false);
        TraitSpawnIndexSnapshot.PresetTraitView preset = new TraitSpawnIndexSnapshot.PresetTraitView(
                id("l2hostility:adaptive"), id("l2hostility:adaptive"), 1, 2, false, 0.75,
                20, id("minecraft:adventure/root"), id("example:preset"), "{}", List.of(constraint));
        TraitSpawnIndexSnapshot.PoolTraitView pool = new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility:speedy"), id("l2hostility:speedy"), 100, 5, 10, 3);
        TraitSpawnIndexSnapshot.BlockedTraitView blocked = new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility:growth"), id("l2hostility:growth"), List.of(
                new TraitSpawnIndexSnapshot.BlockedContextView(id("example:blocked"),
                        List.of(TraitBlockReason.TRAIT_ENTITY_BLACKLIST))));
        return new TraitSpawnIndexSnapshot(revision, List.of(
                new TraitSpawnIndexSnapshot.MobTraitOverview(id("minecraft:zombie"), List.of(config),
                        List.of(preset), List.of(pool), List.of(blocked), List.of(constraint))), 2);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
