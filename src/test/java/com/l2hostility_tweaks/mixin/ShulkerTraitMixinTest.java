package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShulkerTraitMixinTest {

	@Test
	void detectsFriendlyCandidatesFromEitherAllianceDirectionOrOwnership() {
		assertFalse(MixinTestInvoker.<Boolean>call(ShulkerTraitMixin.class,
				"l2fix$isFriendlyCandidate", false, false, false));
		assertTrue(MixinTestInvoker.<Boolean>call(ShulkerTraitMixin.class,
				"l2fix$isFriendlyCandidate", true, false, false));
		assertTrue(MixinTestInvoker.<Boolean>call(ShulkerTraitMixin.class,
				"l2fix$isFriendlyCandidate", false, true, false));
		assertTrue(MixinTestInvoker.<Boolean>call(ShulkerTraitMixin.class,
				"l2fix$isFriendlyCandidate", false, false, true));
	}

	@Test
	void candidateScanRejectsCreativeAndSpectatorTargetsBeforeGeometryWork() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/ShulkerTraitMixin.java"));
		int scan = source.indexOf("player.level().getEntities(player, box,");
		int vanillaFilter = source.indexOf("EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e)", scan);
		int geometry = source.indexOf("Vec3 toEntity", scan);

		assertTrue(scan >= 0);
		assertTrue(vanillaFilter > scan);
		assertTrue(geometry > vanillaFilter);
	}

	@Test
	void candidateScanRejectsPlayersBlockedByPvpRulesBeforeGeometryWork() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/ShulkerTraitMixin.java"));
		int scan = source.indexOf("player.level().getEntities(player, box,");
		int pvpFilter = source.indexOf("!player.canHarmPlayer(candidate)", scan);
		int geometry = source.indexOf("Vec3 toEntity", scan);

		assertTrue(scan >= 0);
		assertTrue(pvpFilter > scan);
		assertTrue(geometry > pvpFilter);
	}

	@Test
	void playerOwnedBulletRequiresAValidLivingNonFriendlyTarget() {
		assertTrue(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				true, false, true, false, false, false));
		assertFalse(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				false, false, true, false, false, false));
		assertFalse(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				true, true, true, false, false, false));
		assertFalse(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				true, false, false, false, false, false));
		assertFalse(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				true, false, true, true, false, false));
		assertFalse(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				true, false, true, false, true, false));
		assertFalse(MixinTestInvoker.<Boolean>call(HostilityBulletMixin.class, "l2fix$isValidPlayerOwnedTarget",
				true, false, true, false, false, true));
	}

	@Test
	void playerOwnedBulletRuleIsWiredBeforeTheNonPlayerFallback() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/HostilityBulletMixin.java"));
		int nullOwner = source.indexOf("if (owner == null) return;");
		int playerOwner = source.indexOf("if (owner instanceof Player)", nullOwner);
		int playerRule = source.indexOf(
				"cir.setReturnValue(l2fix$isValidPlayerOwnedTarget(", playerOwner);
		int playerReturn = source.indexOf("return;", playerRule);
		int nonPlayerFallback = source.indexOf("if (cir.getReturnValue())", playerReturn);

		assertTrue(nullOwner >= 0);
		assertTrue(playerOwner > nullOwner);
		assertTrue(playerRule > playerOwner);
		assertTrue(playerReturn > playerRule);
		assertTrue(nonPlayerFallback > playerReturn);
	}

	@Test
	void playerOwnedBulletRejectsPlayersBlockedByPvpRules() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/HostilityBulletMixin.java"));
		int playerOwner = source.indexOf("if (owner instanceof Player");
		int pvpFilter = source.indexOf("canHarmPlayer(candidate)", playerOwner);
		int playerRule = source.indexOf(
				"cir.setReturnValue(l2fix$isValidPlayerOwnedTarget(", playerOwner);

		assertTrue(playerOwner >= 0);
		assertTrue(pvpFilter > playerOwner);
		assertTrue(playerRule > pvpFilter);
	}

	@Test
	void abrahadabraOwnerResolutionFallsBackToOnlinePlayerAcrossDimensions() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/AbrahadabraReflectMixin.java"));
		int sameLevelLookup = source.indexOf("sl.getEntity(uuid)");
		int onlinePlayerLookup = source.indexOf("sl.getServer().getPlayerList().getPlayer(uuid)");
		int returnOnlinePlayer = source.indexOf("if (player != null) return player;", onlinePlayerLookup);
		int notFound = source.indexOf("return null;", onlinePlayerLookup);

		assertTrue(sameLevelLookup >= 0);
		assertTrue(onlinePlayerLookup > sameLevelLookup);
		assertTrue(returnOnlinePlayer > onlinePlayerLookup);
		assertTrue(notFound > returnOnlinePlayer);
		assertFalse(source.contains("getAllLevels()"));
	}
}
