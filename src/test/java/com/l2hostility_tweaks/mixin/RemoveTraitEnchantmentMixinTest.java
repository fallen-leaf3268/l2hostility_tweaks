package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoveTraitEnchantmentMixinTest {

    @Test
    void splitSuppressorAlwaysCreatesPermanentSeal() {
        assertEquals(-1L, RemoveTraitEnchantmentMixin.l2fix$sealExpiry(1));
        assertEquals(-1L, RemoveTraitEnchantmentMixin.l2fix$sealExpiry(99));
    }

    @Test
    void doesNotFallBackToAnotherTraitWhenSplitIsMissing() {
        Map<String, Integer> traits = new LinkedHashMap<>();
        traits.put("l2hostility:speedy", 2);

        assertNull(RemoveTraitEnchantmentMixin.l2fix$findActiveSplit(traits, id -> id));
    }

    @Test
    void selectsOnlyActiveSplitTrait() {
        Map<String, Integer> traits = new LinkedHashMap<>();
        traits.put("l2hostility:speedy", 2);
        traits.put("l2hostility:split", 1);

        assertEquals("l2hostility:split",
                RemoveTraitEnchantmentMixin.l2fix$findActiveSplit(traits, id -> id));
    }

    @Test
    void doesNotFallBackWhenSplitIsAlreadySealed() {
        Map<String, Integer> traits = new LinkedHashMap<>();
        traits.put("l2hostility:split", -1);
        traits.put("l2hostility:speedy", 2);

        assertNull(RemoveTraitEnchantmentMixin.l2fix$findActiveSplit(traits, id -> id));
    }
}
