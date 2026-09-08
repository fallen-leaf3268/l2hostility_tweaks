package com.l2hostility_tweaks.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JeiResourcesTest {

    private static final Pattern KEY = Pattern.compile(
            "jei\\.l2hostility_tweaks\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)*") ;

    @Test
    void jeiIsAnOptionalClientDependencyWithBoundedVersion() throws IOException {
        String toml = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        int dependency = toml.indexOf("modId=\"jei\"");
        assertTrue(dependency >= 0);
        String block = toml.substring(dependency, Math.min(toml.length(), dependency + 180));
        assertTrue(block.contains("mandatory=false"));
        assertTrue(block.contains("versionRange=\"[15.2,16)\""));
        assertTrue(block.contains("side=\"CLIENT\""));
    }

    @Test
    void bothLocalesCoverEveryJeiKeyUsedByTheCategory() throws IOException {
        Set<String> required = new LinkedHashSet<>();
        for (String file : new String[]{"TraitOverviewCategory.java", "TraitOverviewPresentation.java"}) {
            Matcher matcher = KEY.matcher(Files.readString(Path.of(
                    "src/main/java/com/l2hostility_tweaks/compat/jei/" + file)));
            while (matcher.find()) required.add(matcher.group());
        }
        required.addAll(Set.of(
                "jei.l2hostility_tweaks.dynamic.budget",
                "jei.l2hostility_tweaks.dynamic.legendary_limit",
                "jei.l2hostility_tweaks.dynamic.level_cap",
                "jei.l2hostility_tweaks.dynamic.exclusion",
                "jei.l2hostility_tweaks.dynamic.runtime_allow",
                "jei.l2hostility_tweaks.dynamic.suppression",
                "jei.l2hostility_tweaks.dynamic.condition",
                "jei.l2hostility_tweaks.dynamic.condition_level",
                "jei.l2hostility_tweaks.dynamic.advancement"));

        for (String locale : new String[]{"en_us", "zh_cn"}) {
            JsonObject translations = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/l2hostility_tweaks/lang/" + locale + ".json"))).getAsJsonObject();
            Set<String> missing = required.stream().filter(key -> !translations.has(key))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            assertTrue(missing.isEmpty(), locale + " missing " + missing);
        }
    }

    @Test
    void traitOverviewUsesRichCompleteListsAndShadowlessText() throws IOException {
        String category = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));
        String playerScreen = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java"));
        String clientEvents = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/ClientEventHandler.java"));

        assertTrue(category.contains("TraitListTooltip.exclusiveMarker(TraitOverviewPresentation.poolTraitIds(recipe))"));
        assertTrue(category.contains("TraitListTooltip.marker(TraitOverviewPresentation.blockedTraitIds(recipe))"));
        assertTrue(category.contains("TraitOverviewPresentation.guaranteedPresets(recipe)"));
        assertTrue(category.contains("TraitOverviewPresentation.guaranteedPresetRank(preset)"));
        assertTrue(category.contains("TraitListTooltip.textMarker"));
        assertTrue(category.contains("Component.translatable(\"jei.l2hostility_tweaks.guaranteed_traits\")"));
        assertTrue(category.contains("addConfigTooltip(recipe, tooltip)"));
        assertTrue(category.contains("addTooltipCallback((slot, tooltip) -> addPresetTooltip(recipe, slot, tooltip))"));
        assertFalse(category.contains("drawCenteredString"));
        assertFalse(category.contains("addPoolTooltip"));
        assertTrue(category.contains("isInsideMobPreview"));
        assertFalse(playerScreen.contains("drawCenteredString"));
        assertTrue(clientEvents.contains("event.register(TraitListTooltip.class, TraitListTooltipRenderer::new)"));
        assertTrue(clientEvents.contains("TraitListTooltip.fromMarker"));
        assertTrue(clientEvents.contains("replaceTooltip()"));
        assertTrue(clientEvents.contains("elements.clear()"));
    }
}
