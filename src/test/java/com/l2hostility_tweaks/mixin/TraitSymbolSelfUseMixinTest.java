package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSymbolSelfUseMixinTest {

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
