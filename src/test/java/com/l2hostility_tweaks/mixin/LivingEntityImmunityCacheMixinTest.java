package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingEntityImmunityCacheMixinTest {

	@Test
	void stampsPublishCachedResultsAcrossThreads() throws ReflectiveOperationException {
		var forceStamp = LivingEntityImmunityCacheMixin.class
				.getDeclaredField("l2fix$forceImmunityStamp");
		var gravityStamp = LivingEntityImmunityCacheMixin.class
				.getDeclaredField("l2fix$gravityImmunityStamp");

		assertTrue(Modifier.isVolatile(forceStamp.getModifiers()));
		assertTrue(Modifier.isVolatile(gravityStamp.getModifiers()));
	}

	@Test
	void sameStampScansEachImmunityOnlyOnce() {
		var cache = new CountingCache();
		cache.forceValue = true;
		cache.gravityValue = false;

		assertTrue(cache.l2fix$isImmuneToForce(10L));
		assertTrue(cache.l2fix$isImmuneToForce(10L));
		assertFalse(cache.l2fix$isImmuneToGravity(10L));
		assertFalse(cache.l2fix$isImmuneToGravity(10L));
		assertEquals(1, cache.forceScans);
		assertEquals(1, cache.gravityScans);
	}

	@Test
	void changedStampRefreshesBothIndependentCaches() {
		var cache = new CountingCache();
		cache.forceValue = true;
		cache.gravityValue = true;

		assertTrue(cache.l2fix$isImmuneToForce(20L));
		assertTrue(cache.l2fix$isImmuneToGravity(20L));
		cache.forceValue = false;
		cache.gravityValue = false;
		assertFalse(cache.l2fix$isImmuneToForce(21L));
		assertFalse(cache.l2fix$isImmuneToGravity(21L));
		assertEquals(2, cache.forceScans);
		assertEquals(2, cache.gravityScans);
	}

	@Test
	void differentEntitiesKeepIndependentResultsAtSameStamp() {
		var first = new CountingCache();
		var second = new CountingCache();
		first.forceValue = true;
		second.forceValue = false;

		assertTrue(first.l2fix$isImmuneToForce(30L));
		assertFalse(second.l2fix$isImmuneToForce(30L));
		assertTrue(first.l2fix$isImmuneToForce(30L));
		assertFalse(second.l2fix$isImmuneToForce(30L));
		assertEquals(1, first.forceScans);
		assertEquals(1, second.forceScans);
	}

	private static final class CountingCache extends LivingEntityImmunityCacheMixin {

		private int forceScans;
		private int gravityScans;
		private boolean forceValue;
		private boolean gravityValue;

		@Override
		boolean l2fix$scanForceImmunity() {
			forceScans++;
			return forceValue;
		}

		@Override
		boolean l2fix$scanGravityImmunity() {
			gravityScans++;
			return gravityValue;
		}
	}
}
