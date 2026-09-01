package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoveTraitEnchantmentMixinTest {

    @Test
    void splitSuppressorAlwaysCreatesPermanentSeal() {
        assertEquals(-1L, TraitDisableHelper.permanentSealExpiry(1));
        assertEquals(-1L, TraitDisableHelper.permanentSealExpiry(99));
    }

    @Test
    void doesNotFallBackToAnotherTraitWhenSplitIsMissing() {
        Map<String, Integer> traits = new LinkedHashMap<>();
        traits.put("l2hostility:speedy", 2);

        assertNull(TraitDisableHelper.findActiveSplitTrait(traits, id -> id));
    }

    @Test
    void selectsOnlyActiveSplitTrait() {
        Map<String, Integer> traits = new LinkedHashMap<>();
        traits.put("l2hostility:speedy", 2);
        traits.put("l2hostility:split", 1);

        assertEquals("l2hostility:split",
                TraitDisableHelper.findActiveSplitTrait(traits, id -> id));
    }

    @Test
    void doesNotFallBackWhenSplitIsAlreadySealed() {
        Map<String, Integer> traits = new LinkedHashMap<>();
        traits.put("l2hostility:split", -1);
        traits.put("l2hostility:speedy", 2);

        assertNull(TraitDisableHelper.findActiveSplitTrait(traits, id -> id));
    }
}
