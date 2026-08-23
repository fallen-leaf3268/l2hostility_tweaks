package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitGeneratorMixinTest {

    @Test
    void appliesNbtPresetOnlyWhenItRaisesTheCurrentRank() {
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(0, 2));
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(1, 3));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(3, 1));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(2, 2));
    }
}
