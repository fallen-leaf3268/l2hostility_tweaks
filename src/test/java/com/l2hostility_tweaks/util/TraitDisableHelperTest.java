package com.l2hostility_tweaks.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void clearsUndyingCountAfterUndyingIsUnsealed() {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 3);

        TraitDisableHelper.onTraitUnsealed(data, TraitDisableHelper.UNDYING_TRAIT_ID);

        assertFalse(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void keepsUndyingCountWhenAnotherTraitIsUnsealed() {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 3);

        TraitDisableHelper.onTraitUnsealed(data, TRAIT_ID);

        assertTrue(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
        assertEquals(3, data.getInt(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void hidesUndyingLimitDetailWhenUnlimitedOrSealingIsDisabled() {
        assertNull(TraitDisableHelper.buildUndyingLimitDetail(-1, 60));
        assertNull(TraitDisableHelper.buildUndyingLimitDetail(3, 0));
    }

    @Test
    void buildsTimedUndyingLimitDetail() {
        Component detail = TraitDisableHelper.buildUndyingLimitDetail(3, 60);

        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, detail.getContents());
        assertEquals("trait.l2hostility_tweaks.undying.limit_timed", contents.getKey());
        assertArrayEquals(new Object[]{3, 60}, contents.getArgs());
    }

    @Test
    void buildsPermanentUndyingLimitDetail() {
        Component detail = TraitDisableHelper.buildUndyingLimitDetail(3, -1);

        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, detail.getContents());
        assertEquals("trait.l2hostility_tweaks.undying.limit_permanent", contents.getKey());
        assertArrayEquals(new Object[]{3}, contents.getArgs());
    }
}
