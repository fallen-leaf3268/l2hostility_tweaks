package com.l2hostility_tweaks.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
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
    void traitOverviewUsesCycledSlotDetailsAndShadowlessText() throws IOException {
        String category = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));
        String playerScreen = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java"));
        String clientEvents = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/ClientEventHandler.java"));

        assertFalse(category.contains("TraitListTooltip.exclusiveMarker"));
        assertFalse(category.contains("TraitListTooltip.marker"));
        assertFalse(category.contains("TraitOverviewPresentation.guaranteedPresets(recipe)"));
        assertTrue(category.contains("addConfigTooltip(recipe, tooltip)"));
        assertFalse(category.contains("addPoolTooltip(recipe, slot, tooltip);"));
        assertFalse(category.contains("addBlockedTooltip(recipe, slot, tooltip);"));
        assertFalse(category.contains("addPresetTooltip(recipe, slot, tooltip);"));
        assertFalse(category.contains("drawCenteredString"));
        assertTrue(category.contains("isInsideMobPreview"));
        assertFalse(category.contains("currentBlocked("));
        assertFalse(playerScreen.contains("drawCenteredString"));
        assertTrue(clientEvents.contains("event.register(TraitListTooltip.class, TraitListTooltipRenderer::new)"));
        assertTrue(clientEvents.contains("TraitListTooltip.fromMarker"));
        assertTrue(clientEvents.contains("replaceTooltip()"));
        assertTrue(clientEvents.contains("elements.clear()"));
    }

    @Test
    void overviewCardOnlyShowsTheSharedDifficultyRangeAndUsesTeleportIcon() throws IOException {
        String category = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));
        String constructor = category.substring(category.indexOf("public TraitOverviewCategory"),
                category.indexOf("public RecipeType"));
        String draw = category.substring(category.indexOf("public void draw"),
                category.indexOf("public List<Component> getTooltipStrings"));

        assertFalse(draw.contains("jei.l2hostility_tweaks.config_source"));
        assertFalse(draw.contains("jei.l2hostility_tweaks.dynamic"));
        assertTrue(draw.contains("TraitOverviewPresentation.difficultyRange(config)"));
        assertTrue(draw.contains("jei.l2hostility_tweaks.difficulty_range"));
        assertTrue(category.contains("new ResourceLocation(\"l2hostility\", \"teleport\")"));
        assertTrue(constructor.contains("createDrawableItemStack"));
        assertTrue(category.contains("return icon;"));
    }

    @Test
    void mobTooltipUsesRequestedConfigOrderWithoutAggregatePresetTraits() throws IOException {
        String category = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));
        String mobTooltip = category.substring(category.indexOf("private List<Component> mobTooltip"),
                category.indexOf("private static void addConfigTooltip"));
        String configTooltip = category.substring(category.indexOf("private static void addConfigTooltip"),
                category.indexOf("private static List<ItemStack> resolveStacks"));

        assertTrue(mobTooltip.contains("addConfigTooltip(recipe, tooltip)"));
        assertFalse(mobTooltip.contains("addGuaranteedPresetTooltip"));
        assertAppearsInOrder(configTooltip, List.of(
                "formatEntityConfigPath(config.sourceId())",
                "jei.l2hostility_tweaks.difficulty_range",
                "jei.l2hostility_tweaks.variation",
                "jei.l2hostility_tweaks.scale",
                "jei.l2hostility_tweaks.max_trait_count",
                "jei.l2hostility_tweaks.apply_chance",
                "jei.l2hostility_tweaks.preset_only"));
        assertFalse(configTooltip.contains("jei.l2hostility_tweaks.trait_chance"));
        assertFalse(configTooltip.contains("jei.l2hostility_tweaks.suppression"));
        assertFalse(configTooltip.contains("jei.l2hostility_tweaks.min_spawn_level"));
        assertFalse(configTooltip.contains("jei.l2hostility_tweaks.max_level"));
        assertFalse(configTooltip.contains("jei.l2hostility_tweaks.condition"));
    }

    @Test
    void traitSlotsLeaveItemTooltipsToJei() throws IOException {
        String category = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));
        String setRecipe = category.substring(category.indexOf("public void setRecipe"),
                category.indexOf("public void draw"));

        assertTrue(setRecipe.contains("setSlotName(\"presets\")"));
        assertTrue(setRecipe.contains("setSlotName(\"pool\")"));
        assertTrue(setRecipe.contains("setSlotName(\"blocked\")"));
        assertFalse(setRecipe.contains("addPresetTooltip"));
        assertFalse(setRecipe.contains("addPoolTooltip"));
        assertFalse(setRecipe.contains("addBlockedTooltip"));
        assertFalse(category.contains("private static void addPresetTooltip"));
        assertFalse(category.contains("private static void addPoolTooltip"));
        assertFalse(category.contains("private static void addBlockedTooltip"));
    }

    @Test
    void overviewAddsThreeCycledEquipmentSlotsBetweenMobAndTraits() throws IOException {
        String category = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));

        assertTrue(category.contains("EQUIPMENT_SLOT_X = 77"));
        assertTrue(category.contains("ARMOR_SLOT_Y = 12"));
        assertTrue(category.contains("HANDS_SLOT_Y = 42"));
        assertTrue(category.contains("CURIOS_SLOT_Y = 72"));
        assertTrue(category.contains("\"equipment_armor\""));
        assertTrue(category.contains("\"equipment_hands\""));
        assertTrue(category.contains("\"equipment_curios\""));
        assertTrue(category.contains("setSlotName(name)"));
        assertTrue(category.contains("entry.stack()"));
    }

    private static void assertAppearsInOrder(String source, List<String> fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int current = source.indexOf(fragment);
            assertTrue(current > previous, fragment + " is missing or out of order");
            previous = current;
        }
    }

}
