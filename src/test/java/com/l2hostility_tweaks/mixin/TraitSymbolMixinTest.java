package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitSymbolMixinTest {

    @Test
    void displaysInvalidTraitIdAsLiteralText() {
        assertEquals("Invalid Trait ID",
                TraitSymbolMixin.l2fix$getTraitName(null, "Invalid Trait ID").getString());
    }

    @Test
    void preservesPreRefreshHealthPercentage() {
        assertEquals(30.0f, TraitSymbolMixin.l2fix$scaledHealth(60.0f, 100.0f, 50.0f), 0.0001f);
    }

    @Test
    void clampsRestoredHealthToExistingBounds() {
        assertEquals(50.0f, TraitSymbolMixin.l2fix$scaledHealth(120.0f, 100.0f, 50.0f));
        assertEquals(1.0f, TraitSymbolMixin.l2fix$scaledHealth(0.0f, 100.0f, 50.0f));
    }
}
