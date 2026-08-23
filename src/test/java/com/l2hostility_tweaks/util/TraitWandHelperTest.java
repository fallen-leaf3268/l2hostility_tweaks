package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitWandHelperTest {

    @Test
    void dropsOnlyTheRemainderAfterPartialInventoryInsertion() {
        AtomicInteger remaining = new AtomicInteger(16);
        AtomicInteger dropped = new AtomicInteger();

        boolean delivered = TraitWandHelper.deliver(
                () -> remaining.addAndGet(-3),
                () -> remaining.get() > 0,
                () -> {
                    dropped.set(remaining.getAndSet(0));
                    return true;
                });

        assertTrue(delivered);
        assertEquals(13, dropped.get());
        assertEquals(0, remaining.get());
    }

    @Test
    void skipsDropAfterCompleteInventoryInsertion() {
        AtomicInteger remaining = new AtomicInteger(16);
        AtomicBoolean dropped = new AtomicBoolean();

        boolean delivered = TraitWandHelper.deliver(
                () -> remaining.set(0),
                () -> remaining.get() > 0,
                () -> {
                    dropped.set(true);
                    return true;
                });

        assertTrue(delivered);
        assertFalse(dropped.get());
    }

    @Test
    void reportsFailureWhenRemainderCannotBeDropped() {
        AtomicInteger remaining = new AtomicInteger(16);

        boolean delivered = TraitWandHelper.deliver(
                () -> {},
                () -> remaining.get() > 0,
                () -> false);

        assertFalse(delivered);
        assertEquals(16, remaining.get());
    }
}
