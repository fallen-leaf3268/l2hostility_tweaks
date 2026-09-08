package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitRuleClassifierTest {
    @Test
    void reportsEveryIndependentAbsoluteBlock() {
        var context = new TraitRuleClassifier.Context(
                true, true, true, true, true, false,
                false, false, false, false);

        assertEquals(List.of(
                TraitBlockReason.ENTITY_CONFIG_BLACKLIST,
                TraitBlockReason.TRAIT_ENTITY_BLACKLIST,
                TraitBlockReason.TRAIT_WHITELIST_MISS,
                TraitBlockReason.TRAIT_GLOBALLY_DISABLED,
                TraitBlockReason.ENTITY_NO_TRAIT), TraitRuleClassifier.classify(context));
    }

    @Test
    void presetOnlyAndTweaksRandomDisableDoNotBlockPresetEntries() {
        var preset = new TraitRuleClassifier.Context(
                false, false, false, false, false, true,
                true, true, false, false);

        assertTrue(TraitRuleClassifier.classify(preset).isEmpty());
    }

    @Test
    void disableAllAndDisableMobLevelBlockPresetsAndPool() {
        var context = new TraitRuleClassifier.Context(
                false, false, false, false, false, false,
                false, false, true, true);

        assertEquals(List.of(
                TraitBlockReason.TWEAKS_DISABLE_ALL,
                TraitBlockReason.TWEAKS_DISABLE_MOB_LEVEL), TraitRuleClassifier.classify(context));
    }

    @Test
    void runtimeEntityRejectionBlocksBothPresetAndRandomEntries() {
        var random = new TraitRuleClassifier.Context(
                false, false, false, false, false, true, false,
                false, false, false, false);
        var preset = new TraitRuleClassifier.Context(
                false, false, false, false, false, true, true,
                false, false, false, false);

        assertEquals(List.of(TraitBlockReason.TRAIT_RUNTIME_REJECTED),
                TraitRuleClassifier.classify(random));
        assertEquals(List.of(TraitBlockReason.TRAIT_RUNTIME_REJECTED),
                TraitRuleClassifier.classify(preset));
    }
}
