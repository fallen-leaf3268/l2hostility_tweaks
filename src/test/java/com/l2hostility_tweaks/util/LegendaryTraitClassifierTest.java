package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegendaryTraitClassifierTest {

    @Test
    void recognizesNativeAndConfiguredLegendaryTraits() {
        Set<String> extraIds = Set.of("addon:extra");

        assertTrue(LegendaryTraitClassifier.classify(true, "l2hostility:native", Set.of()));
        assertTrue(LegendaryTraitClassifier.classify(false, "addon:extra", extraIds));
        assertFalse(LegendaryTraitClassifier.classify(false, "addon:normal", extraIds));
        assertFalse(LegendaryTraitClassifier.classify(false, null, extraIds));
    }

    @Test
    void distinguishesConfiguredTraitsFromNativeLegendaryTraits() {
        Set<String> extraIds = Set.of("addon:extra", "l2hostility:native");

        assertTrue(LegendaryTraitClassifier.classifyExtra(false, "addon:extra", extraIds));
        assertFalse(LegendaryTraitClassifier.classifyExtra(true, "l2hostility:native", extraIds));
        assertFalse(LegendaryTraitClassifier.classifyExtra(false, "addon:normal", extraIds));
        assertFalse(LegendaryTraitClassifier.classifyExtra(false, null, extraIds));
    }

    @Test
    void allLegendaryConsumersUseTheCentralClassifier() throws Exception {
        String generation = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/TraitGenerationHelper.java"));
        String postRoll = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitPostRollMixin.java"));
        String overlay = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));
        String symbol = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolMixin.java"));

        assertTrue(generation.contains("LegendaryTraitClassifier.isLegendary(entry.getKey())"));
        assertTrue(postRoll.contains("LegendaryTraitClassifier.isLegendary(trait)"));
        assertTrue(postRoll.contains("LegendaryTraitClassifier.isLegendary(e.getKey())"));
        assertTrue(overlay.contains("LegendaryTraitClassifier.isDisplayLegendary(entry.getKey())"));
        assertTrue(symbol.contains("LegendaryTraitClassifier.isDisplayExtraLegendary(trait)"));

        for (String source : Set.of(generation, postRoll, overlay, symbol)) {
            assertFalse(source.contains("instanceof LegendaryTrait ||"));
        }
    }

    @Test
    void reportsInvalidAndMissingConfiguredIds() {
        Set<String> unknown = LegendaryTraitClassifier.unknownIds(
                Set.of("addon:known", "addon:missing", "not valid"),
                id -> id.toString().equals("addon:known"));

        assertEquals(Set.of("addon:missing", "not valid"), unknown);
        assertTrue(LegendaryTraitClassifier.unknownIds(Set.of(), id -> false).isEmpty());
    }

    @Test
    void validatesIdsAfterRegistrationAndConfigReload() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        String call = "LegendaryTraitClassifier.validateConfiguredIds();";

        assertEquals(2, source.split(java.util.regex.Pattern.quote(call), -1).length - 1);
    }
}
