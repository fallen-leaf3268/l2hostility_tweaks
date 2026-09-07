package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class TraitSpawnClientCache {

    public static final TraitSpawnClientCache INSTANCE = new TraitSpawnClientCache();

    private static final TraitSpawnIndexSnapshot DISCONNECTED = new TraitSpawnIndexSnapshot(Long.MIN_VALUE, List.of(), 0);

    private final List<Consumer<TraitSpawnIndexSnapshot>> listeners = new CopyOnWriteArrayList<>();
    private volatile TraitSpawnIndexSnapshot current = DISCONNECTED;

    public synchronized boolean install(TraitSpawnIndexSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.revision() <= current.revision()) return false;
        current = snapshot;
        publish(snapshot);
        return true;
    }

    public synchronized void clear() {
        current = DISCONNECTED;
        publish(DISCONNECTED);
    }

    public TraitSpawnIndexSnapshot current() {
        return current;
    }

    public void addListener(Consumer<TraitSpawnIndexSnapshot> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private void publish(TraitSpawnIndexSnapshot snapshot) {
        for (Consumer<TraitSpawnIndexSnapshot> listener : listeners) {
            listener.accept(snapshot);
        }
    }
}
