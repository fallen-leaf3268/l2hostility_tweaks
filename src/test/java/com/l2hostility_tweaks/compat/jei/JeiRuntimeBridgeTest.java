package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeiRuntimeBridgeTest {

    @Test
    void refreshAppliesIngredientDeltaAndReplacesPagesOnce() {
        FakeRuntimeAccess access = new FakeRuntimeAccess();
        JeiRuntimeBridge bridge = new JeiRuntimeBridge();
        bridge.runtimeAvailable(access);
        bridge.refresh(snapshot(1, "minecraft:zombie"));
        bridge.refresh(snapshot(2, "minecraft:skeleton"));

        assertEquals(List.of("minecraft:zombie"), access.removedMobIds);
        assertEquals(List.of("minecraft:zombie", "minecraft:skeleton"), access.addedMobIds);
        assertEquals(1, access.hiddenBatches);
        assertEquals(2, access.addedPageBatches);
    }

    @Test
    void runtimeReconnectReplaysTheLatestSnapshotWithoutRepeatingEqualRevisions() {
        JeiRuntimeBridge bridge = new JeiRuntimeBridge();
        bridge.refresh(snapshot(4, "minecraft:zombie"));

        FakeRuntimeAccess first = new FakeRuntimeAccess();
        bridge.runtimeAvailable(first);
        bridge.refresh(snapshot(4, "minecraft:skeleton"));
        bridge.runtimeUnavailable();

        FakeRuntimeAccess second = new FakeRuntimeAccess();
        bridge.runtimeAvailable(second);

        assertEquals(List.of("minecraft:zombie"), first.addedMobIds);
        assertEquals(1, first.addedPageBatches);
        assertEquals(List.of("minecraft:zombie"), second.addedMobIds);
        assertEquals(1, second.addedPageBatches);
    }

    @Test
    void clearingSnapshotRemovesIngredientsAndHidesPreviousPages() {
        FakeRuntimeAccess access = new FakeRuntimeAccess();
        JeiRuntimeBridge bridge = new JeiRuntimeBridge();
        bridge.runtimeAvailable(access);
        bridge.refresh(snapshot(1, "minecraft:zombie", "minecraft:skeleton"));
        bridge.refresh(snapshot(Long.MIN_VALUE));

        assertEquals(List.of("minecraft:zombie", "minecraft:skeleton"), access.removedMobIds);
        assertEquals(1, access.hiddenBatches);
        assertEquals(1, access.addedPageBatches);
    }

    @Test
    void equalPagesRemainVisibleAfterRevisionRefresh() {
        FakeRuntimeAccess access = new FakeRuntimeAccess();
        JeiRuntimeBridge bridge = new JeiRuntimeBridge();
        bridge.runtimeAvailable(access);

        bridge.refresh(snapshot(1, "minecraft:zombie"));
        bridge.refresh(snapshot(2, "minecraft:zombie"));

        assertEquals(1, access.visiblePageCount());
    }

    private static TraitSpawnIndexSnapshot snapshot(long revision, String... entityIds) {
        List<TraitSpawnIndexSnapshot.MobTraitOverview> pages = List.of(entityIds).stream()
                .map(ResourceLocation::new)
                .map(id -> new TraitSpawnIndexSnapshot.MobTraitOverview(
                        id, List.of(), List.of(), List.of(), List.of(), List.of()))
                .toList();
        return new TraitSpawnIndexSnapshot(revision, pages, 0);
    }

    private static final class FakeRuntimeAccess implements JeiRuntimeAccess {
        private final List<String> addedMobIds = new ArrayList<>();
        private final List<String> removedMobIds = new ArrayList<>();
        private final Set<TraitSpawnIndexSnapshot.MobTraitOverview> pages = new HashSet<>();
        private final Set<TraitSpawnIndexSnapshot.MobTraitOverview> hiddenPages = new HashSet<>();
        private int hiddenBatches;
        private int addedPageBatches;

        @Override
        public void addMobIngredients(Collection<MobIngredient> ingredients) {
            ingredients.stream().map(MobIngredient::entityId).map(ResourceLocation::toString)
                    .forEach(addedMobIds::add);
        }

        @Override
        public void removeMobIngredients(Collection<MobIngredient> ingredients) {
            ingredients.stream().map(MobIngredient::entityId).map(ResourceLocation::toString)
                    .forEach(removedMobIds::add);
        }

        @Override
        public void hidePages(Collection<TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
            hiddenBatches++;
            hiddenPages.addAll(pages);
        }

        @Override
        public void addPages(List<TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
            addedPageBatches++;
            pages.stream().filter(page -> !hiddenPages.contains(page)).forEach(this.pages::add);
        }

        @Override
        public void unhidePages(Collection<TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
            hiddenPages.removeAll(pages);
        }

        private int visiblePageCount() {
            return (int) pages.stream().filter(page -> !hiddenPages.contains(page)).count();
        }
    }
}
