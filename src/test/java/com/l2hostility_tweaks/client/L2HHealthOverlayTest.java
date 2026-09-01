package com.l2hostility_tweaks.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

	@Test
	void entityRayEndsAtTheFirstBlockingSurface() throws Exception {
		Vec3 fullReach = new Vec3(0, 0, 50);
		BlockHitResult wall = new BlockHitResult(
				new Vec3(0, 0, 4), Direction.NORTH, BlockPos.ZERO, false);
		BlockHitResult miss = BlockHitResult.miss(fullReach, Direction.NORTH, BlockPos.ZERO);

		assertEquals(wall.getLocation(), L2HHealthOverlay.l2fix$visibleRayEnd(fullReach, wall));
		assertEquals(fullReach, L2HHealthOverlay.l2fix$visibleRayEnd(fullReach, miss));

		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));
		int blockRay = source.indexOf("mc.level.clip(new ClipContext(");
		int visibleEnd = source.indexOf("l2fix$visibleRayEnd(reachVec, blockHit)", blockRay);
		int entityRay = source.indexOf("ProjectileUtil.getEntityHitResult(", visibleEnd);
		assertTrue(blockRay >= 0);
		assertTrue(visibleEnd > blockRay);
		assertTrue(entityRay > visibleEnd);
	}

	@Test
	void hudVisibilitySwitchBelongsToClientConfig() throws Exception {
		String commonConfig = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/config/L2HConfig.java"));
		String clientConfig = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/client/config/ClientL2HConfig.java"));
		String overlay = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));
		String namePlate = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/RenderNamePlateMixin.java"));

		assertFalse(commonConfig.contains("public final ForgeConfigSpec.BooleanValue showHud;"));
		assertFalse(commonConfig.contains("showHud = builder.comment"));
		assertTrue(clientConfig.contains("public final ForgeConfigSpec.BooleanValue showHud;"));
		assertTrue(clientConfig.contains("showHud = builder.comment(\"显示自定义血条 HUD\")"));
		assertEquals(2, overlay.split("ClientL2HConfig.CLIENT.showHud.get\\(\\)", -1).length - 1);
		assertFalse(overlay.contains("L2HConfig.COMMON.showHud.get()"));
		assertTrue(namePlate.contains("ClientL2HConfig.CLIENT.showHud.get()"));
		assertFalse(namePlate.contains("L2HConfig.COMMON.showHud.get()"));
	}

	@Test
	void precomputeReadsCurrentBossEventsBeforeResolvingHudState() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));
		int precompute = source.indexOf("public static void precomputeHudState()");
		int currentBossState = source.indexOf(
				"bossEventsActive = !((BossHealthOverlayAccessor) mc.gui.getBossOverlay()).getEvents().isEmpty();",
				precompute);
		int resolve = source.indexOf("l2fix$resolveHudState(hasValidTarget, bossEventsActive,", precompute);

		assertTrue(precompute >= 0);
		assertTrue(currentBossState > precompute);
		assertTrue(resolve > currentBossState);
	}
}
