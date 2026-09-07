package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;

import java.util.ArrayDeque;
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
    private final ArrayDeque<Notification> notifications = new ArrayDeque<>();
    private volatile TraitSpawnIndexSnapshot current = DISCONNECTED;
    private boolean publishing;

    public boolean install(TraitSpawnIndexSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean drain;
        synchronized (this) {
            if (snapshot.revision() <= current.revision()) return false;
            current = snapshot;
            drain = enqueue(snapshot);
        }
        if (drain) drainNotifications();
        return true;
    }

    public void clear() {
        boolean drain;
        synchronized (this) {
            current = DISCONNECTED;
            drain = enqueue(DISCONNECTED);
        }
        if (drain) drainNotifications();
    }

    public TraitSpawnIndexSnapshot current() {
        return current;
    }

    public void addListener(Consumer<TraitSpawnIndexSnapshot> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private boolean enqueue(TraitSpawnIndexSnapshot snapshot) {
        notifications.addLast(new Notification(snapshot, List.copyOf(listeners)));
        if (publishing) return false;
        publishing = true;
        return true;
    }

    private void drainNotifications() {
        while (true) {
            Notification notification;
            synchronized (this) {
                notification = notifications.pollFirst();
                if (notification == null) {
                    publishing = false;
                    return;
                }
            }
            publish(notification);
        }
    }

    private void publish(Notification notification) {
        for (Consumer<TraitSpawnIndexSnapshot> listener : notification.listeners()) {
            try {
                listener.accept(notification.snapshot());
            } catch (RuntimeException exception) {
                LOGGER.warn("Trait spawn index listener failed", exception);
            }
        }
    }

    private record Notification(TraitSpawnIndexSnapshot snapshot,
                                List<Consumer<TraitSpawnIndexSnapshot>> listeners) {
    }
}
