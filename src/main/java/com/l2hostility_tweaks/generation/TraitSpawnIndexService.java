package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class TraitSpawnIndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:trait_spawn_index");

    private final AtomicBoolean rebuildRequested = new AtomicBoolean();
    private final SnapshotSource source;
    private final Consumer<TraitSpawnIndexSnapshot> broadcaster;
    private volatile TraitSpawnIndexSnapshot current = new TraitSpawnIndexSnapshot(0, List.of(), 0);

    public TraitSpawnIndexService(SnapshotSource source, Consumer<TraitSpawnIndexSnapshot> broadcaster) {
        this.source = Objects.requireNonNull(source);
        this.broadcaster = Objects.requireNonNull(broadcaster);
    }

    public TraitSpawnIndexService() {
        this(server -> TraitSpawnIndexBuilder.build(0, MinecraftTraitSpawnIndexSource.capture(server)),
                ignored -> {});
    }

    public void requestRebuild() {
        rebuildRequested.set(true);
    }

    public synchronized boolean rebuildIfRequested(MinecraftServer server) {
        if (!rebuildRequested.getAndSet(false)) return false;

        TraitSpawnIndexSnapshot captured;
        try {
            captured = Objects.requireNonNull(source.capture(server));
        } catch (Exception exception) {
            LOGGER.error("Failed to rebuild trait spawn index", exception);
            return false;
        }

        long nextRevision = current.revision() + 1;
        TraitSpawnIndexSnapshot replacement = new TraitSpawnIndexSnapshot(
                nextRevision, captured.mobs(), captured.warningCount());
        current = replacement;
        try {
            broadcaster.accept(replacement);
        } catch (Exception exception) {
            LOGGER.error("Failed to broadcast trait spawn index revision {}", nextRevision, exception);
        }
        return true;
    }

    public TraitSpawnIndexSnapshot current() {
        return current;
    }

    @FunctionalInterface
    public interface SnapshotSource {
        TraitSpawnIndexSnapshot capture(MinecraftServer server);
    }
}
