package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitGeneratorMixinTest {

    @Test
    void appliesNbtPresetOnlyWhenItRaisesTheCurrentRank() {
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(0, 2));
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(1, 3));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(3, 1));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(2, 2));
    }

    @Test
    void parsesValidNbtPresetTraitId() {
        assertEquals("l2hostility:tank",
                TraitGeneratorMixin.l2fix$parseTraitId("l2hostility:tank").toString());
    }

    @Test
    void rejectsMalformedNbtPresetTraitIdWithoutThrowing() {
        assertNull(TraitGeneratorMixin.l2fix$parseTraitId("Invalid Trait ID"));
    }

    @Test
    void rejectsNullNbtPresetTraitId() {
        assertNull(TraitGeneratorMixin.l2fix$parseTraitId(null));
    }
}
