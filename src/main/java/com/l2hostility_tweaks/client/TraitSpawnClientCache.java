package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TraitSpawnClientCache {

    public static final TraitSpawnClientCache INSTANCE = new TraitSpawnClientCache();

    private static final Logger LOGGER = LoggerFactory.getLogger(TraitSpawnClientCache.class);
    private static final TraitSpawnIndexSnapshot DISCONNECTED = new TraitSpawnIndexSnapshot(Long.MIN_VALUE, List.of(), 0);

    private final List<Consumer<TraitSpawnIndexSnapshot>> listeners = new CopyOnWriteArrayList<>();
    private volatile TraitSpawnIndexSnapshot current = DISCONNECTED;

    public boolean install(TraitSpawnIndexSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<Consumer<TraitSpawnIndexSnapshot>> snapshotListeners;
        synchronized (this) {
            if (snapshot.revision() <= current.revision()) return false;
            current = snapshot;
            snapshotListeners = List.copyOf(listeners);
        }
        publish(snapshot, snapshotListeners);
        return true;
    }

    public void clear() {
        List<Consumer<TraitSpawnIndexSnapshot>> snapshotListeners;
        synchronized (this) {
            current = DISCONNECTED;
            snapshotListeners = List.copyOf(listeners);
        }
        publish(DISCONNECTED, snapshotListeners);
    }

    public TraitSpawnIndexSnapshot current() {
        return current;
    }

    public void addListener(Consumer<TraitSpawnIndexSnapshot> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private void publish(TraitSpawnIndexSnapshot snapshot,
                         List<Consumer<TraitSpawnIndexSnapshot>> snapshotListeners) {
        for (Consumer<TraitSpawnIndexSnapshot> listener : snapshotListeners) {
            try {
                listener.accept(snapshot);
            } catch (RuntimeException exception) {
                LOGGER.warn("Trait spawn index listener failed", exception);
            }
        }
    }
}
