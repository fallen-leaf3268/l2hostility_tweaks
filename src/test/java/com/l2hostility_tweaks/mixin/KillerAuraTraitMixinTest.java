package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillerAuraTraitMixinTest {

	@Test
	void alwaysRejectsHolderItself() {
		assertFalse(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", true, true, true, true, true));
	}

	@Test
	void targetsPlayerOnlyWhenPvpAllowsIt() {
		assertTrue(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", false, true, false, false, false));
		assertFalse(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", false, false, false, false, false));
	}

	@Test
	void targetsMobAttackingHolder() {
		assertTrue(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", false, false, true, false, false));
	}

	@Test
	void targetsCurrentTargetOfMobHolder() {
		assertTrue(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", false, false, false, true, false));
	}

	@Test
	void targetsMobRecentlyHitByPlayerHolder() {
		assertTrue(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", false, false, false, false, true));
	}

	@Test
	void rejectsEntityWithoutTargetRelationship() {
		assertFalse(MixinTestInvoker.<Boolean>call(KillerAuraTraitMixin.class, "l2fix$shouldTarget", false, false, false, false, false));
	}
}
