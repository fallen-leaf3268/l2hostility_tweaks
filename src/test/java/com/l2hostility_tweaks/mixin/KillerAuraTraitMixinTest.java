package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillerAuraTraitMixinTest {

	@Test
	void alwaysRejectsHolderItself() {
		assertFalse(KillerAuraTraitMixin.l2fix$shouldTarget(true, true, true, true, true));
	}

	@Test
	void targetsOtherNonCreativePlayer() {
		assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, true, false, false, false));
	}

	@Test
	void targetsMobAttackingHolder() {
		assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, true, false, false));
	}

	@Test
	void targetsCurrentTargetOfMobHolder() {
		assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, false, true, false));
	}

	@Test
	void targetsMobRecentlyHitByPlayerHolder() {
		assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, false, false, true));
	}

	@Test
	void rejectsEntityWithoutTargetRelationship() {
		assertFalse(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, false, false, false));
	}
}
