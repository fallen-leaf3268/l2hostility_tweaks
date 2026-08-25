package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDetectorGlassesCacheMixinTest {

	@Test
	void cachesWithinTickAndRefreshesOnNextTick() {
		var cache = new CountingCache();
		cache.scannedValue = true;

		assertTrue(cache.l2fix$hasDetectorGlassesAtTick(12));
		assertTrue(cache.l2fix$hasDetectorGlassesAtTick(12));
		assertEquals(1, cache.scanCount);

		cache.scannedValue = false;
		assertFalse(cache.l2fix$hasDetectorGlassesAtTick(13));
		assertEquals(2, cache.scanCount);
	}

	@Test
	void differentPlayerInstancesKeepIndependentValues() {
		var first = new CountingCache();
		var second = new CountingCache();
		first.scannedValue = true;
		second.scannedValue = false;

		assertTrue(first.l2fix$hasDetectorGlassesAtTick(20));
		assertFalse(second.l2fix$hasDetectorGlassesAtTick(20));
		assertEquals(1, first.scanCount);
		assertEquals(1, second.scanCount);
	}

	@Test
	void visibilityUsesRegisteredPlayerInstanceCache() throws IOException {
		String cacheMixin = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/PlayerDetectorGlassesCacheMixin.java"));
		String visibilityMixin = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/EntityInvisibleMixin.java"));
		String mixinConfig = Files.readString(Path.of(
				"src/main/resources/l2hostility_tweaks.mixins.json"));

		assertTrue(cacheMixin.contains("@Mixin(Player.class)"));
		assertTrue(cacheMixin.contains("implements DetectorGlassesCache"));
		assertTrue(mixinConfig.contains("\"PlayerDetectorGlassesCacheMixin\""));
		assertTrue(visibilityMixin.contains(
				"((DetectorGlassesCache) player).l2fix$hasDetectorGlasses()"));
		assertFalse(visibilityMixin.contains("Map<UUID"));
		assertFalse(visibilityMixin.contains("ConcurrentHashMap"));
		assertFalse(visibilityMixin.contains("playerGlassesTick"));
	}

	private static final class CountingCache extends PlayerDetectorGlassesCacheMixin {

		private int scanCount;
		private boolean scannedValue;

		@Override
		boolean l2fix$scanDetectorGlasses() {
			scanCount++;
			return scannedValue;
		}
	}
}
