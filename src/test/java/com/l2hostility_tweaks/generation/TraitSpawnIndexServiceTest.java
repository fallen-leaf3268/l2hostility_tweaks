package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSpawnIndexServiceTest {

    @Test
    void keepsPreviousSnapshotWhenRebuildThrows() {
        AtomicInteger attempts = new AtomicInteger();
        TraitSpawnIndexService service = new TraitSpawnIndexService(server -> {
            if (attempts.getAndIncrement() == 0) return snapshot(99);
            throw new IllegalStateException("broken reload");
        }, ignored -> {});

        service.requestRebuild();
        assertTrue(service.rebuildIfRequested(null));
        service.requestRebuild();
        assertFalse(service.rebuildIfRequested(null));

        assertEquals(1, service.current().revision());
        assertEquals(1, service.current().mobs().size());
    }

    @Test
    void rebuildsOnlyWhenRequestedAndBroadcastsTheReplacedSnapshot() {
        AtomicInteger captures = new AtomicInteger();
        List<TraitSpawnIndexSnapshot> broadcasts = new ArrayList<>();
        TraitSpawnIndexService service = new TraitSpawnIndexService(server -> {
            captures.incrementAndGet();
            return snapshot(-20);
        }, broadcasts::add);

        assertFalse(service.rebuildIfRequested(null));
        service.requestRebuild();
        assertTrue(service.rebuildIfRequested(null));
        service.requestRebuild();
        assertTrue(service.rebuildIfRequested(null));

        assertEquals(2, captures.get());
        assertEquals(2, service.current().revision());
        assertEquals(List.of(1L, 2L), broadcasts.stream().map(TraitSpawnIndexSnapshot::revision).toList());
        assertTrue(broadcasts.get(1) == service.current());
    }

    private static TraitSpawnIndexSnapshot snapshot(long revision) {
        return new TraitSpawnIndexSnapshot(revision, List.of(
                new TraitSpawnIndexSnapshot.MobTraitOverview(
                        new net.minecraft.resources.ResourceLocation("minecraft:zombie"),
                        List.of(), List.of(), List.of(), List.of(), List.of())), 0);
    }
}
