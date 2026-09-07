package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSpawnLifecycleTest {

    @Test
    void serviceCanReportInitializationAndResetItsLifecycle() {
        TraitSpawnIndexService service = new TraitSpawnIndexService(
                server -> new TraitSpawnIndexSnapshot(0, List.of(), 0), ignored -> {});

        assertFalse(service.isInitialized());
        service.requestRebuild();
        assertTrue(service.rebuildIfRequested(null));
        assertTrue(service.isInitialized());
        service.reset();
        assertFalse(service.isInitialized());
    }

    @Test
    void commonEventsRebuildBeforePeriodicWorkAndSynchronizePlayers() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        String request = "TraitSpawnIndexService.INSTANCE.requestRebuild()";
        String rebuild = "TraitSpawnIndexService.INSTANCE.rebuildIfRequested(event.getServer())";

        assertTrue(source.contains("onTagsUpdated(TagsUpdatedEvent event)"));
        assertTrue(source.contains(request));
        assertTrue(source.contains(rebuild));
        assertTrue(source.indexOf(rebuild) < source.indexOf("getTickCount() % 100"));
        assertTrue(source.contains("NetworkHandler.sendTraitSpawnIndexToPlayer(sp"));
        assertTrue(source.contains("TraitSpawnIndexService.INSTANCE.reset()"));
    }
}
