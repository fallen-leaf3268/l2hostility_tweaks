package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.l2hostility_tweaks.network.TraitSpawnIndexCodecTest.completeSnapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSpawnClientCacheTest {

    @Test
    void acceptsNewerRevisionAndIgnoresOlderOrDuplicateSnapshots() {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        AtomicInteger notifications = new AtomicInteger();
        cache.addListener(snapshot -> notifications.incrementAndGet());

        assertTrue(cache.install(completeSnapshot(5)));
        assertFalse(cache.install(completeSnapshot(5)));
        assertFalse(cache.install(completeSnapshot(4)));
        assertEquals(1, notifications.get());
    }

    @Test
    void clearPublishesAnEmptyDisconnectedSnapshot() {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        cache.install(completeSnapshot(2));

        cache.clear();

        TraitSpawnIndexSnapshot current = cache.current();
        assertTrue(current.mobs().isEmpty());
        assertEquals(Long.MIN_VALUE, current.revision());
    }

    @Test
    void notifiesOutsideCacheLock() {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        cache.addListener(snapshot -> assertFalse(Thread.holdsLock(cache)));

        cache.install(completeSnapshot(5));
        cache.clear();
    }

    @Test
    void brokenListenerDoesNotBlockLaterInstallOrClearNotifications() {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        AtomicInteger notifications = new AtomicInteger();
        cache.addListener(snapshot -> {
            throw new IllegalStateException("broken listener");
        });
        cache.addListener(snapshot -> notifications.incrementAndGet());

        assertDoesNotThrow(() -> cache.install(completeSnapshot(5)));
        assertDoesNotThrow(cache::clear);
        assertEquals(2, notifications.get());
    }
}
