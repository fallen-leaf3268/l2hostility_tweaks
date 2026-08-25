package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDetectorGlassesCacheMixinTest {

	@Test
	void cachesWithinTickAndRefreshesOnNextTick() {
		var cache = new PlayerDetectorGlassesCacheMixin();
		cache.l2fix$storeDetectorGlasses(12, true);

		assertTrue(cache.l2fix$isDetectorGlassesCacheValid(12));
		assertFalse(cache.l2fix$isDetectorGlassesCacheValid(13));
		assertTrue(cache.l2fix$getCachedDetectorGlasses());
	}

	@Test
	void differentPlayerInstancesKeepIndependentValues() {
		var first = new PlayerDetectorGlassesCacheMixin();
		var second = new PlayerDetectorGlassesCacheMixin();
		first.l2fix$storeDetectorGlasses(20, true);
		second.l2fix$storeDetectorGlasses(20, false);

		assertTrue(first.l2fix$getCachedDetectorGlasses());
		assertFalse(second.l2fix$getCachedDetectorGlasses());
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
}
