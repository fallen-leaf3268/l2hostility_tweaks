package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Test
    void concurrentInstallsNeverOverlapOneListenerAndPreserveAcceptedOrder() throws Exception {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean inCallback = new AtomicBoolean();
        AtomicBoolean overlapped = new AtomicBoolean();
        List<Long> revisions = Collections.synchronizedList(new ArrayList<>());
        cache.addListener(snapshot -> {
            if (!inCallback.compareAndSet(false, true)) overlapped.set(true);
            try {
                if (snapshot.revision() == 1) {
                    firstEntered.countDown();
                    await(releaseFirst);
                }
                revisions.add(snapshot.revision());
            } finally {
                inCallback.set(false);
            }
        });

        Thread first = new Thread(() -> cache.install(completeSnapshot(1)));
        Thread second = new Thread(() -> cache.install(completeSnapshot(2)));
        first.start();
        assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
        second.start();
        second.join(5000);
        releaseFirst.countDown();
        first.join(5000);

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertFalse(overlapped.get());
        assertEquals(List.of(1L, 2L), revisions);
    }

    @Test
    void concurrentInstallAndClearNeverOverlapOneListener() throws Exception {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        CountDownLatch installEntered = new CountDownLatch(1);
        CountDownLatch releaseInstall = new CountDownLatch(1);
        AtomicBoolean inCallback = new AtomicBoolean();
        AtomicBoolean overlapped = new AtomicBoolean();
        List<Long> revisions = Collections.synchronizedList(new ArrayList<>());
        cache.addListener(snapshot -> {
            if (!inCallback.compareAndSet(false, true)) overlapped.set(true);
            try {
                if (snapshot.revision() == 1) {
                    installEntered.countDown();
                    await(releaseInstall);
                }
                revisions.add(snapshot.revision());
            } finally {
                inCallback.set(false);
            }
        });

        Thread install = new Thread(() -> cache.install(completeSnapshot(1)));
        Thread clear = new Thread(cache::clear);
        install.start();
        assertTrue(installEntered.await(5, TimeUnit.SECONDS));
        clear.start();
        clear.join(5000);
        releaseInstall.countDown();
        install.join(5000);

        assertFalse(install.isAlive());
        assertFalse(clear.isAlive());
        assertFalse(overlapped.get());
        assertEquals(List.of(1L, Long.MIN_VALUE), revisions);
    }

    @Test
    void recursiveListenerMutationIsQueuedBehindCurrentNotification() {
        TraitSpawnClientCache cache = new TraitSpawnClientCache();
        List<Long> observedBySecond = new ArrayList<>();
        cache.addListener(snapshot -> {
            if (snapshot.revision() == 1) cache.install(completeSnapshot(2));
        });
        cache.addListener(snapshot -> observedBySecond.add(snapshot.revision()));

        cache.install(completeSnapshot(1));

        assertEquals(List.of(1L, 2L), observedBySecond);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
