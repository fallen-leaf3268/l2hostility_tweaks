package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitWandHelperTest {

	@Test
	void traitLookupDoesNotCreateItemNbt() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/util/TraitWandHelper.java"));
		int getTrait = source.indexOf("public static MobTrait getTrait");
		int parseTraitId = source.indexOf("static ResourceLocation parseTraitId");
		String getTraitBody = source.substring(getTrait, parseTraitId);

		assertTrue(getTraitBody.contains("stack.getTag()"));
		assertFalse(getTraitBody.contains("stack.getOrCreateTag()"));
	}

	@Test
	void splitsRefundIntoLegalStacks() {
		assertEquals(List.of(64, 63), TraitWandHelper.splitCounts(127, 64));
		assertEquals(List.of(64), TraitWandHelper.splitCounts(64, 64));
		assertTrue(TraitWandHelper.splitCounts(127, 64).stream()
				.allMatch(count -> count > 0 && count <= 64));
	}

	@Test
	void invalidRefundSplitInputsReturnNoStacks() {
		assertEquals(List.of(), TraitWandHelper.splitCounts(0, 64));
		assertEquals(List.of(), TraitWandHelper.splitCounts(-1, 64));
		assertEquals(List.of(), TraitWandHelper.splitCounts(1, 0));
		assertEquals(List.of(), TraitWandHelper.splitCounts(1, -1));
	}

	@Test
	void rejectsRefundsThatExceedOperationStackLimit() {
		assertTrue(TraitWandHelper.isSafeDelivery(4096, 64));
		assertFalse(TraitWandHelper.isSafeDelivery(4097, 64));
		assertFalse(TraitWandHelper.isSafeDelivery(Integer.MAX_VALUE, 64));
		assertEquals(List.of(), TraitWandHelper.splitCounts(4097, 64));
		assertEquals(List.of(), TraitWandHelper.splitCounts(Integer.MAX_VALUE, 64));
	}

    @Test
    void parsesValidTraitId() {
        assertEquals("l2hostility:split",
                TraitWandHelper.parseTraitId("l2hostility:split").toString());
    }

    @Test
    void rejectsInvalidTraitIdWithoutThrowing() {
        assertNull(TraitWandHelper.parseTraitId("Invalid Trait ID"));
    }

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
