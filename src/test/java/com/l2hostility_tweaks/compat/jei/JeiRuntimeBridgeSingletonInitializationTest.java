package com.l2hostility_tweaks.compat.jei;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JeiRuntimeBridgeSingletonInitializationTest {

    @Test
    void singletonCanAcceptRuntimeOnFirstClassInitialization() {
        assertDoesNotThrow(() -> JeiRuntimeBridge.INSTANCE.runtimeAvailable(new FakeRuntimeAccess()));
    }

    private static final class FakeRuntimeAccess implements JeiRuntimeAccess {
        @Override
        public void addMobIngredients(java.util.Collection<MobIngredient> ingredients) {
        }

        @Override
        public void removeMobIngredients(java.util.Collection<MobIngredient> ingredients) {
        }

        @Override
        public void hidePages(java.util.Collection<com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
        }

        @Override
        public void addPages(java.util.List<com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
        }
    }
}
