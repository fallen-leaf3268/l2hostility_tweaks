package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveTraitEnchantmentMixinTest {

    @Test
    void splitSuppressorAlwaysCreatesPermanentSeal() {
        assertEquals(-1L, RemoveTraitEnchantmentMixin.l2fix$sealExpiry(1));
        assertEquals(-1L, RemoveTraitEnchantmentMixin.l2fix$sealExpiry(99));
    }
}
