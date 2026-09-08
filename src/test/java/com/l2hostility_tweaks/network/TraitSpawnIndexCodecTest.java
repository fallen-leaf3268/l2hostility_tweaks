package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
    void roundTripsConditionalPageVariantIndex() {
        TraitSpawnIndexSnapshot original = completeSnapshot(12);
        TraitSpawnIndexSnapshot.MobTraitOverview page = original.mobs().get(0);
        TraitSpawnIndexSnapshot expected = new TraitSpawnIndexSnapshot(12, List.of(
                new TraitSpawnIndexSnapshot.MobTraitOverview(
                        page.entityId(), 2, page.configs(), page.presets(), page.pool(), page.blocked(),
                        page.dynamicConstraints())), original.warningCount());

        TraitSpawnIndexSnapshot decoded = TraitSpawnIndexCodec.decode(TraitSpawnIndexCodec.encode(expected));

        assertEquals(2, decoded.mobs().get(0).variantIndex());
        assertEquals(expected, decoded);
    }

    @Test
    void rejectsOversizedMobListBeforeAllocation() {
        CompoundTag root = new CompoundTag();
        root.putLong("revision", 1);
        root.putInt("mobCount", TraitSpawnIndexCodec.MAX_MOBS + 1);

        assertThrows(IllegalArgumentException.class, () -> TraitSpawnIndexCodec.decode(root));
    }

    @Test
    void rejectsNonemptyListWithWrongElementTypeEvenWhenDeclaredCountIsZero() {
        CompoundTag root = new CompoundTag();
        root.putLong("revision", 1);
        root.putInt("warningCount", 0);
        root.putInt("mobCount", 0);
        ListTag mobs = new ListTag();
        mobs.add(StringTag.valueOf("not a compound"));
        root.put("mobs", mobs);

        assertThrows(IllegalArgumentException.class, () -> TraitSpawnIndexCodec.decode(root));
    }

    @Test
    void rejectsInvalidProbabilityAndResourceId() {
        assertThrows(IllegalArgumentException.class,
                () -> TraitSpawnIndexCodec.validateProbability(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> TraitSpawnIndexCodec.parseId("bad id"));
    }

    @Test
    void roundTripsUpstreamDefaultMaxTraitCountSentinel() {
        TraitSpawnIndexSnapshot expected = withMaxTraitCount(completeSnapshot(10), -1);

        assertEquals(expected, TraitSpawnIndexCodec.decode(TraitSpawnIndexCodec.encode(expected)));
        assertThrows(IllegalArgumentException.class,
                () -> TraitSpawnIndexCodec.encode(withMaxTraitCount(completeSnapshot(11), -2)));
    }

    private static TraitSpawnIndexSnapshot withMaxTraitCount(TraitSpawnIndexSnapshot original, int maxTraitCount) {
        TraitSpawnIndexSnapshot.MobTraitOverview mob = original.mobs().get(0);
        TraitSpawnIndexSnapshot.EntityConfigView config = mob.configs().get(0);
        TraitSpawnIndexSnapshot.EntityConfigView sentinelConfig = new TraitSpawnIndexSnapshot.EntityConfigView(
                config.sourceId(), config.conditionJson(), config.minDifficulty(), config.baseDifficulty(),
                config.variation(), config.scale(), config.applyChance(), config.traitChance(),
                config.suppression(), config.minSpawnLevel(), config.maxLevel(), maxTraitCount,
                config.presetTraitsOnly());
        return new TraitSpawnIndexSnapshot(original.revision(), List.of(
                new TraitSpawnIndexSnapshot.MobTraitOverview(mob.entityId(), List.of(sentinelConfig),
                        mob.presets(), mob.pool(), mob.blocked(), mob.dynamicConstraints())),
                original.warningCount());
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
