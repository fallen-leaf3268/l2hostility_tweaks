package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetTraitLevelMixinTest {

    @Test
    void onlyPositiveRawLevelsAreActive() {
        assertTrue(GetTraitLevelMixin.l2fix$isActiveLevel(1));
        assertFalse(GetTraitLevelMixin.l2fix$isActiveLevel(0));
        assertFalse(GetTraitLevelMixin.l2fix$isActiveLevel(-1));
    }
}
