package com.l2hostility_tweaks.util;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TraitDisableHelperTest {

    private static final String TRAIT_ID = "l2hostility:split";
    private static final String SEALED_LEVEL_KEY = "l2htweaks_sealed_level_" + TRAIT_ID;

    @Test
    void rebuildsSealedLevelFromNegativeRawLevel() {
        CompoundTag data = new CompoundTag();

        TraitDisableHelper.syncSealedLevelData(data, TRAIT_ID, -3);

        assertEquals(3, data.getInt(SEALED_LEVEL_KEY));
    }

    @Test
    void removesStaleSealedLevelForPositiveRawLevel() {
        CompoundTag data = new CompoundTag();
        data.putInt(SEALED_LEVEL_KEY, 3);

        TraitDisableHelper.syncSealedLevelData(data, TRAIT_ID, 2);

        assertFalse(data.contains(SEALED_LEVEL_KEY));
    }

    @Test
    void clearsSealedLevelAndExpiryTogether() {
        CompoundTag data = new CompoundTag();
        String expiryKey = TraitDisableHelper.sealExpiryKey(TRAIT_ID);
        data.putInt(SEALED_LEVEL_KEY, 3);
        data.putLong(expiryKey, 1200L);

        TraitDisableHelper.clearSealData(data, TRAIT_ID);

        assertFalse(data.contains(SEALED_LEVEL_KEY));
        assertFalse(data.contains(expiryKey));
    }
}
