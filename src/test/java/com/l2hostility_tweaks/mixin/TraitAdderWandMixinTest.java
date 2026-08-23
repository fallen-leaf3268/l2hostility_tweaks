package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitAdderWandMixinTest {

    @Test
    void preservesSealedMaxLevelTraitOnNormalClick() {
        assertTrue(TraitAdderWandMixin.l2fix$shouldPreserveSealedState(false, 3, 3));
        assertFalse(TraitAdderWandMixin.l2fix$shouldPreserveSealedState(true, 3, 3));
        assertFalse(TraitAdderWandMixin.l2fix$shouldPreserveSealedState(false, 2, 3));
    }
}
