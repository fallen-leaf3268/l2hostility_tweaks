package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitAdderWandMixinTest {

    @Test
    void sealedTraitRecoveryUsesCentralRuntimeCleanup() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitAdderWandMixin.java"));

        assertTrue(source.contains("TraitDisableHelper.clearSealData(target.getPersistentData(), trait.getID())"));
        assertFalse(source.contains("remove(TraitDisableHelper.sealExpiryKey"));
        assertFalse(source.contains("remove(\"l2htweaks_sealed_level_\""));
    }

    @Test
    void preservesSealedMaxLevelTraitOnNormalClick() {
        assertTrue(TraitAdderWandMixin.l2fix$shouldPreserveSealedState(false, 3, 3));
        assertFalse(TraitAdderWandMixin.l2fix$shouldPreserveSealedState(true, 3, 3));
        assertFalse(TraitAdderWandMixin.l2fix$shouldPreserveSealedState(false, 2, 3));
    }

    @Test
    void sealedTraitRecoveryDelegatesLifecycleAndSyncToTheOriginalMethod() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitAdderWandMixin.java"));

        assertTrue(source.contains("cap.traits.put(trait, abs)"));
        assertFalse(source.contains("trait.initialize("));
        assertFalse(source.contains("trait.postInit("));
        assertFalse(source.contains("cap.syncToClient("));
    }
}
