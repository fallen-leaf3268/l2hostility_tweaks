package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerAntibuildBypassCacheMixinTest {

	@Test
	void cachesWithinGameTickAndRefreshesOnNextTick() {
		var cache = new CountingCache();
		cache.scannedValue = true;

		assertTrue(cache.l2fix$hasAntibuildBypassAtTime(100L));
		assertTrue(cache.l2fix$hasAntibuildBypassAtTime(100L));
		assertEquals(1, cache.scanCount);

		cache.scannedValue = false;
		assertFalse(cache.l2fix$hasAntibuildBypassAtTime(101L));
		assertEquals(2, cache.scanCount);
	}

	@Test
	void differentPlayerInstancesKeepIndependentValues() {
		var first = new CountingCache();
		var second = new CountingCache();
		first.scannedValue = true;
		second.scannedValue = false;

		assertTrue(first.l2fix$hasAntibuildBypassAtTime(200L));
		assertFalse(second.l2fix$hasAntibuildBypassAtTime(200L));
		assertEquals(1, first.scanCount);
		assertEquals(1, second.scanCount);
	}

	@Test
	void bothCallersUseRegisteredPlayerInstanceCache() throws IOException {
		String cacheMixin = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/PlayerAntibuildBypassCacheMixin.java"));
		String blockMixin = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/AntibuildBlockImmuneMixin.java"));
		String placeMixin = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/AntibuildPlaceBypassMixin.java"));
		String mixinConfig = Files.readString(Path.of(
				"src/main/resources/l2hostility_tweaks.mixins.json"));

		assertTrue(cacheMixin.contains("@Mixin(Player.class)"));
		assertTrue(cacheMixin.contains("implements AntibuildBypassCache"));
		assertTrue(mixinConfig.contains("\"PlayerAntibuildBypassCacheMixin\""));
		assertTrue(blockMixin.contains(
				"((AntibuildBypassCache) (Object) this).l2fix$hasAntibuildBypass()"));
		assertTrue(placeMixin.contains(
				"((AntibuildBypassCache) player).l2fix$hasAntibuildBypass()"));
		assertFalse(blockMixin.contains("hasArenaTrait"));
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/l2hostility_tweaks/util/AntibuildBypassHelper.java")));
	}

	private static final class CountingCache extends PlayerAntibuildBypassCacheMixin {

		private int scanCount;
		private boolean scannedValue;

		@Override
		boolean l2fix$scanAntibuildBypass() {
			scanCount++;
			return scannedValue;
		}
	}
}
