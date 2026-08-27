package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.EntityImmunityCache;
import com.l2hostility_tweaks.util.ImmunityHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingEntityImmunityCacheMixinTest {

	@Test
	void stampsPublishCachedResultsAcrossThreads() throws ReflectiveOperationException {
		var forceStamp = LivingEntityImmunityCacheMixin.class
				.getDeclaredField("l2fix$forceImmunityStamp");
		var gravityStamp = LivingEntityImmunityCacheMixin.class
				.getDeclaredField("l2fix$gravityImmunityStamp");
		var combatCurioStamp = LivingEntityImmunityCacheMixin.class
				.getDeclaredField("l2fix$combatCurioStamp");
		var dimensionBreakerStamp = LivingEntityImmunityCacheMixin.class
				.getDeclaredField("l2fix$dimensionBreakerStamp");

		assertTrue(Modifier.isVolatile(forceStamp.getModifiers()));
		assertTrue(Modifier.isVolatile(gravityStamp.getModifiers()));
		assertTrue(Modifier.isVolatile(combatCurioStamp.getModifiers()));
		assertTrue(Modifier.isVolatile(dimensionBreakerStamp.getModifiers()));
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

	@Test
	void combatCuriosScanOncePerStampAndRefreshImmediatelyAfterInvalidation() {
		var cache = new CountingCache();
		var first = new ImmunityHelper.CombatCurioSnapshot(
				true, false, false, List.of(0.65f, 1.25f));
		var second = new ImmunityHelper.CombatCurioSnapshot(
				false, true, true, List.of(1.25f));
		cache.combatValue = first;

		assertSame(first, cache.l2fix$getCombatCurios(40L));
		assertSame(first, cache.l2fix$getCombatCurios(40L));
		assertEquals(1, cache.combatScans);

		cache.combatValue = second;
		cache.l2fix$invalidateCombatCurios();
		assertSame(second, cache.l2fix$getCombatCurios(40L));
		assertEquals(2, cache.combatScans);
	}

	@Test
	void dimensionBreakerConsumersShareStateAndAllEquipmentChangesInvalidateIt() throws IOException {
		String breaker = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/content/DimensionBreakerItem.java"));
		String main = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
		String network = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));
		String helper = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java"));

		assertTrue(breaker.contains("getEquippedState(LivingEntity entity)"));
		assertTrue(breaker.contains("ImmunityHelper.getDimensionBreakerState(entity)"));
		assertTrue(helper.contains("l2fix$getDimensionBreakerState(entity.tickCount)"));
		assertTrue(breaker.contains("return getEquippedState(entity).equipped();"));
		assertTrue(breaker.contains("return getEquippedState(player).protectActive();"));
		assertTrue(main.contains("LivingEquipmentChangeEvent"));
		assertTrue(main.contains("ImmunityHelper.invalidateDimensionBreaker(event.getEntity())"));
		assertTrue(network.contains("ImmunityHelper.invalidateDimensionBreaker(player)"));
	}

	@Test
	void dimensionBreakerScansOncePerStampAndRefreshesImmediatelyAfterInvalidation() {
		var cache = new CountingCache();
		cache.dimensionBreakerValue = EntityImmunityCache.DimensionBreakerState.EQUIPPED;

		assertSame(EntityImmunityCache.DimensionBreakerState.EQUIPPED,
				cache.l2fix$getDimensionBreakerState(50L));
		assertSame(EntityImmunityCache.DimensionBreakerState.EQUIPPED,
				cache.l2fix$getDimensionBreakerState(50L));
		assertEquals(1, cache.dimensionBreakerScans);

		cache.dimensionBreakerValue = EntityImmunityCache.DimensionBreakerState.PROTECTED;
		cache.l2fix$invalidateDimensionBreaker();
		assertSame(EntityImmunityCache.DimensionBreakerState.PROTECTED,
				cache.l2fix$getDimensionBreakerState(50L));
		assertEquals(2, cache.dimensionBreakerScans);
	}

	private static final class CountingCache extends LivingEntityImmunityCacheMixin {

		private int forceScans;
		private int gravityScans;
		private int combatScans;
		private int dimensionBreakerScans;
		private boolean forceValue;
		private boolean gravityValue;
		private ImmunityHelper.CombatCurioSnapshot combatValue =
				ImmunityHelper.CombatCurioSnapshot.EMPTY;
		private EntityImmunityCache.DimensionBreakerState dimensionBreakerValue =
				EntityImmunityCache.DimensionBreakerState.EMPTY;

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

		@Override
		ImmunityHelper.CombatCurioSnapshot l2fix$scanCombatCurios() {
			combatScans++;
			return combatValue;
		}

		@Override
		EntityImmunityCache.DimensionBreakerState l2fix$scanDimensionBreakerState() {
			dimensionBreakerScans++;
			return dimensionBreakerValue;
		}
	}
}
