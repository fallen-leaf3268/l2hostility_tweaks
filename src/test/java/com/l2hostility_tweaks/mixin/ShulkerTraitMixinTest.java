package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShulkerTraitMixinTest {

	@Test
	void detectsFriendlyCandidatesFromEitherAllianceDirectionOrOwnership() {
		assertFalse(ShulkerTraitMixin.l2fix$isFriendlyCandidate(false, false, false));
		assertTrue(ShulkerTraitMixin.l2fix$isFriendlyCandidate(true, false, false));
		assertTrue(ShulkerTraitMixin.l2fix$isFriendlyCandidate(false, true, false));
		assertTrue(ShulkerTraitMixin.l2fix$isFriendlyCandidate(false, false, true));
	}
}
