package com.l2hostility_tweaks.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class L2HHealthOverlayTest {

	@Test
	void resolvesHudAndBossbarVisibilityFromOneStateMatrix() {
		assertEquals(new L2HHealthOverlay.HudState(false, false),
				L2HHealthOverlay.l2fix$resolveHudState(false, false, false));
		assertEquals(new L2HHealthOverlay.HudState(true, true),
				L2HHealthOverlay.l2fix$resolveHudState(true, false, true));
		assertEquals(new L2HHealthOverlay.HudState(false, false),
				L2HHealthOverlay.l2fix$resolveHudState(true, true, true));
		assertEquals(new L2HHealthOverlay.HudState(true, true),
				L2HHealthOverlay.l2fix$resolveHudState(true, true, false));
	}

	@Test
	void traitCacheKeyMatchesEveryRenderDependencyByIdentity() throws Exception {
		Class<?> keyType = Arrays.stream(L2HHealthOverlay.class.getDeclaredClasses())
				.filter(type -> type.getSimpleName().equals("TraitCacheKey"))
				.findFirst().orElse(null);
		assertNotNull(keyType);

		var constructor = keyType.getDeclaredConstructor(
				int.class, int.class, int.class, boolean.class, Object.class, Object.class);
		constructor.setAccessible(true);
		Object legendaryIds = new Object();
		Object language = new Object();
		Object key = constructor.newInstance(7, 11, 120, true, legendaryIds, language);
		var matches = keyType.getDeclaredMethod("matches",
				int.class, int.class, int.class, boolean.class, Object.class, Object.class);
		matches.setAccessible(true);

		assertTrue((boolean) matches.invoke(key, 7, 11, 120, true, legendaryIds, language));
		assertFalse((boolean) matches.invoke(key, 8, 11, 120, true, legendaryIds, language));
		assertFalse((boolean) matches.invoke(key, 7, 12, 120, true, legendaryIds, language));
		assertFalse((boolean) matches.invoke(key, 7, 11, 121, true, legendaryIds, language));
		assertFalse((boolean) matches.invoke(key, 7, 11, 120, false, legendaryIds, language));
		assertFalse((boolean) matches.invoke(key, 7, 11, 120, true, new Object(), language));
		assertFalse((boolean) matches.invoke(key, 7, 11, 120, true, legendaryIds, new Object()));
	}

	@Test
	void hudWiresAllDependenciesIntoTheTraitCacheKey() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));

		assertTrue(source.contains("Language.getInstance()"));
		int cacheGuard = source.indexOf("if (cachedTraitKey == null || !cachedTraitKey.matches(");
		int scan = source.indexOf("scanTraits(", cacheGuard);
		int allocation = source.indexOf("new TraitCacheKey(", cacheGuard);
		int renderContinues = source.indexOf("Minecraft mc = Minecraft.getInstance();", cacheGuard);
		assertTrue(cacheGuard >= 0);
		assertTrue(scan > cacheGuard);
		assertTrue(allocation > scan);
		assertTrue(renderContinues > allocation);
		assertEquals(1, source.split("new TraitCacheKey\\(", -1).length - 1);
	}
}
