package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSymbolSelfUseMixinTest {

	@Test
	void selfUseUsesCentralRuntimeCleanup() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java"));

		assertTrue(source.contains("TraitDisableHelper.clearSealData(player.getPersistentData(), trait.getID())"));
		assertFalse(source.contains("TraitDisableHelper.SEAL_EXPIRY_PREFIX + trait.getID()"));
		assertFalse(source.contains("remove(\"l2htweaks_sealed_level_\""));
	}

	@Test
	void serverRejectsUnpayableCostBeforeInventoryMutation() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java"));

		assertTrue(source.contains("L2HConfig.getUpgradeCost(currentLevel, stack.getMaxStackSize())"));
		int rejection = source.indexOf("cost == TraitCostHelper.UNPAYABLE");
		assertTrue(rejection >= 0);
		assertTrue(rejection < source.indexOf("stack.getCount() < cost"));
		assertTrue(rejection < source.indexOf("stack.shrink(cost)"));
		assertFalse(source.contains("1 << currentLevel"));
	}

	@Test
	void clientUsesSharedCostAndUnavailableTooltip() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java"));

		assertTrue(source.contains(
				"L2HConfig.getDisplayUpgradeCost(curLevel, e.owner().asItem().getMaxStackSize())"));
		assertTrue(source.contains("L2HTweaksLang.UPGRADE_UNPAYABLE"));
		assertFalse(source.contains("1 << curLevel"));
	}

	@Test
	void clientPredictionReturnsBeforeServerAuthorityChecks() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java"));

		int traitSymbolCheck = source.indexOf("stack.getItem() instanceof TraitSymbol traitSymbol");
		int shiftCheck = source.indexOf("if (!player.isShiftKeyDown()) return;");
		int clientGuard = source.indexOf("if (level.isClientSide())");
		int configGate = source.indexOf("if (!L2HConfig.isPlayerSelfTraitEnabled()) return;");
		int blacklistCheck = source.indexOf("if (ImmunityHelper.isSelfBlacklisted(trait))");
		int costCheck = source.indexOf("int cost = L2HConfig.getUpgradeCost(");

		assertTrue(traitSymbolCheck >= 0 && traitSymbolCheck < shiftCheck);
		assertTrue(shiftCheck < clientGuard);
		assertTrue(clientGuard < configGate);
		assertTrue(clientGuard < blacklistCheck);
		assertTrue(clientGuard < costCheck);
		assertTrue(source.substring(clientGuard, configGate).contains(
				"cir.setReturnValue(InteractionResultHolder.success(stack))"));
	}

    @Test
    void sealedRawLevelParticipatesInMaximumLevelCheck() {
        assertTrue(TraitSymbolSelfUseMixin.l2fix$isAtMaxLevel(-3, 3));
        assertFalse(TraitSymbolSelfUseMixin.l2fix$isAtMaxLevel(-2, 3));
    }

    @Test
    void sealedEntriesCountAsExistingTraits() {
        assertEquals(3, TraitSymbolSelfUseMixin.l2fix$projectedTraitCount(List.of(1, -2, -1), -2));
        assertEquals(4, TraitSymbolSelfUseMixin.l2fix$projectedTraitCount(List.of(1, -2, -1), null));
    }

    @Test
    void sealedTraitParticipatesInExclusionValidation() {
        assertTrue(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(1));
        assertTrue(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(-1));
        assertFalse(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(0));
        assertFalse(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(null));
    }
}
