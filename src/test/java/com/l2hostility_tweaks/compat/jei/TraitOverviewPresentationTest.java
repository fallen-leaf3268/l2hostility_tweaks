package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.client.TraitListTooltip;
import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitOverviewPresentationTest {

    @Test
    void searchableOutputsContainPresetAndPoolButNeverBlockedTraits() {
        TraitSpawnIndexSnapshot.MobTraitOverview page = overviewWithPresetPoolAndBlocked();

        Set<ResourceLocation> outputIds = TraitOverviewPresentation.searchableOutputItemIds(page);

        assertEquals(Set.of(id("l2hostility:adaptive"), id("l2hostility:speedy")), outputIds);
        assertFalse(outputIds.contains(id("l2hostility:growth")));
    }

    @Test
    void blockedCarouselIsDeterministicAndHandlesEmptyLists() {
        List<String> blocked = List.of("first", "second", "third");

        assertEquals("first", TraitOverviewPresentation.cycle(blocked, 0));
        assertEquals("third", TraitOverviewPresentation.cycle(blocked, 2));
        assertEquals("first", TraitOverviewPresentation.cycle(blocked, 3));
        assertEquals("third", TraitOverviewPresentation.cycle(blocked, -1));
        assertNull(TraitOverviewPresentation.cycle(List.of(), 4));
    }

    @Test
    void guaranteedPresetsUseServerComputedRanks() {
        var guaranteed = preset(id("l2hostility:adaptive"), 2, 4, 1.0, 0, null, "", List.of());
        var strongerGuaranteed = preset(id("l2hostility:adaptive"), 3, 4, 1.0, 0, null, "", List.of());
        var minimumOnly = preset(id("l2hostility:speedy"), 0, 1, 1.0, 0, null, "", List.of());
        var random = preset(id("l2hostility:tank"), 1, 1, 0.5, 0, null, "", List.of());
        var levelBound = preset(id("l2hostility:growth"), 1, 1, 1.0, 20, null, "", List.of());
        var advancementBound = preset(id("l2hostility:undying"), 1, 1, 1.0, 0,
                id("minecraft:story/mine_stone"), "", List.of());
        var conditionalConfig = preset(id("l2hostility:invisible"), 1, 1, 1.0, 0, null,
                "{\"nbt\":{}}", List.of());
        var runtimeBound = preset(id("l2hostility:teleport"), 1, 1, 1.0, 0, null, "",
                List.of(new TraitDynamicConstraint("runtime_allow", List.of("l2hostility:teleport"))));
        var page = new TraitSpawnIndexSnapshot.MobTraitOverview(id("minecraft:zombie"), List.of(),
                List.of(guaranteed, strongerGuaranteed, minimumOnly, random, levelBound, advancementBound,
                        conditionalConfig, runtimeBound), List.of(), List.of(), List.of());

        assertEquals(List.of(strongerGuaranteed, minimumOnly),
                TraitOverviewPresentation.guaranteedPresets(page));
        assertEquals(4, TraitOverviewPresentation.guaranteedPresetRank(strongerGuaranteed));
        assertEquals(1, TraitOverviewPresentation.guaranteedPresetRank(minimumOnly));
    }

    @Test
    void conditionalPageDoesNotTreatRuntimeAllowAsSatisfied() {
        String condition = "{\"nbt\":{\"isApollyon\":1}}";
        var undying = preset(id("l2hostility:undying"), 1, 1, 1.0, 0, null,
                condition, List.of(new TraitDynamicConstraint(
                        "runtime_allow", List.of("l2hostility:undying"))));
        var page = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("goety:apostle"), 1, List.of(configWithCondition(condition)),
                List.of(undying), List.of(), List.of(), List.of());

        assertEquals(List.of(), TraitOverviewPresentation.guaranteedPresets(page));
    }

    @Test
    void displayedPresetEntriesFollowTheCurrentlyCycledItem() {
        var adaptiveFirst = preset(id("l2hostility:adaptive"), 1, 1, 0.5, 0, null, "", List.of());
        var speedy = preset(id("l2hostility:speedy"), 2, 2, 1.0, 0, null, "", List.of());
        var adaptiveSecond = preset(id("l2hostility:adaptive"), 3, 3, 1.0, 0, null, "", List.of());
        var page = new TraitSpawnIndexSnapshot.MobTraitOverview(id("minecraft:zombie"), List.of(),
                List.of(adaptiveFirst, speedy, adaptiveSecond), List.of(), List.of(), List.of());

        assertEquals(List.of(adaptiveFirst, adaptiveSecond),
                TraitOverviewPresentation.presetsForItem(page, id("l2hostility:adaptive")));
        assertEquals(List.of(speedy),
                TraitOverviewPresentation.presetsForItem(page, id("l2hostility:speedy")));
        assertEquals(List.of(),
                TraitOverviewPresentation.presetsForItem(page, id("l2hostility:missing")));
    }

    @Test
    void displayedPoolAndBlockedEntriesFollowTheCurrentlyCycledItem() {
        var speedyFirst = new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility:speedy"), id("l2hostility:speedy"), 10, 0, 1, 3);
        var adaptive = new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility:adaptive"), id("l2hostility:adaptive"), 20, 5, 2, 4);
        var speedySecond = new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility:speedy"), id("l2hostility:speedy"), 30, 10, 3, 5);
        var growth = new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility:growth"), id("l2hostility:growth"), List.of());
        var tank = new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility:tank"), id("l2hostility:tank"), List.of());
        var page = new TraitSpawnIndexSnapshot.MobTraitOverview(id("minecraft:zombie"), List.of(), List.of(),
                List.of(speedyFirst, adaptive, speedySecond), List.of(growth, tank), List.of());

        assertEquals(List.of(speedyFirst, speedySecond),
                TraitOverviewPresentation.poolForItem(page, id("l2hostility:speedy")));
        assertEquals(List.of(adaptive),
                TraitOverviewPresentation.poolForItem(page, id("l2hostility:adaptive")));
        assertEquals(List.of(growth),
                TraitOverviewPresentation.blockedForItem(page, id("l2hostility:growth")));
        assertEquals(List.of(),
                TraitOverviewPresentation.blockedForItem(page, id("l2hostility:missing")));
    }

    @Test
    void sectionTraitIdsAreCompleteAndDeduplicatedInSnapshotOrder() {
        var page = new TraitSpawnIndexSnapshot.MobTraitOverview(id("minecraft:zombie"), List.of(), List.of(),
                List.of(
                        new TraitSpawnIndexSnapshot.PoolTraitView(id("l2hostility:speedy"),
                                id("l2hostility:speedy"), 10, 0, 1, 3),
                        new TraitSpawnIndexSnapshot.PoolTraitView(id("l2hostility:adaptive"),
                                id("l2hostility:adaptive"), 10, 0, 1, 3),
                        new TraitSpawnIndexSnapshot.PoolTraitView(id("l2hostility:speedy"),
                                id("l2hostility:speedy"), 10, 0, 1, 3)),
                List.of(
                        new TraitSpawnIndexSnapshot.BlockedTraitView(id("l2hostility:growth"),
                                id("l2hostility:growth"), List.of()),
                        new TraitSpawnIndexSnapshot.BlockedTraitView(id("l2hostility:growth"),
                                id("l2hostility:growth"), List.of())), List.of());

        assertEquals(List.of(id("l2hostility:speedy"), id("l2hostility:adaptive")),
                TraitOverviewPresentation.poolTraitIds(page));
        assertEquals(List.of(id("l2hostility:growth")),
                TraitOverviewPresentation.blockedTraitIds(page));
    }

    @Test
    void textOnlyTraitListMarkerPreservesEveryTraitAndRank() {
        List<TraitListTooltip.Entry> entries = List.of(
                new TraitListTooltip.Entry(id("l2hostility:speedy"), 2),
                new TraitListTooltip.Entry(id("l2hostility:tank"), 1));

        TraitListTooltip parsed = TraitListTooltip.fromMarker(
                TraitListTooltip.textMarker(entries).getString()).orElseThrow();

        assertFalse(parsed.showIcons());
        assertFalse(parsed.replaceTooltip());
        assertFalse(parsed.overheadLayout());
        assertEquals(entries, parsed.entries());
    }

    @Test
    void overheadTraitListMarkerGroupsAtMostThreeTraitsPerLine() {
        List<TraitListTooltip.Entry> entries = List.of(
                new TraitListTooltip.Entry(id("l2hostility:speedy"), 1),
                new TraitListTooltip.Entry(id("l2hostility:tank"), 2),
                new TraitListTooltip.Entry(id("l2hostility:fiery"), 3),
                new TraitListTooltip.Entry(id("l2hostility:poison"), 4),
                new TraitListTooltip.Entry(id("l2hostility:reflect"), 5),
                new TraitListTooltip.Entry(id("l2hostility:gravity"), 6),
                new TraitListTooltip.Entry(id("l2hostility:moonwalk"), 7));

        TraitListTooltip parsed = TraitListTooltip.fromMarker(
                TraitListTooltip.overheadMarker(entries).getString()).orElseThrow();

        assertTrue(parsed.overheadLayout());
        assertFalse(parsed.showIcons());
        assertFalse(parsed.replaceTooltip());
        assertEquals(entries, parsed.entries());
        assertEquals(List.of(3, 3, 1), parsed.rows().stream().map(List::size).toList());
    }

    @Test
    void exclusiveTraitListMarkerReplacesTheOriginalItemTooltip() {
        List<ResourceLocation> traits = List.of(id("l2hostility:speedy"), id("l2hostility:tank"));

        TraitListTooltip parsed = TraitListTooltip.fromMarker(
                TraitListTooltip.exclusiveMarker(traits).getString()).orElseThrow();

        assertTrue(parsed.showIcons());
        assertTrue(parsed.replaceTooltip());
        assertFalse(parsed.overheadLayout());
        assertEquals(traits, parsed.entries().stream().map(TraitListTooltip.Entry::traitId).toList());
    }

    @Test
    void presetTooltipLabelsChanceAsDatapackProbability() {
        List<String> keys = TraitOverviewPresentation.presetTooltipKeys(presetAtHalfChance());

        assertEquals(List.of(
                "jei.l2hostility_tweaks.preset_chance",
                "jei.l2hostility_tweaks.free_rank",
                "jei.l2hostility_tweaks.min_rank",
                "jei.l2hostility_tweaks.cap",
                "jei.l2hostility_tweaks.condition_level",
                "jei.l2hostility_tweaks.advancement",
                "jei.l2hostility_tweaks.condition"), keys);
        assertFalse(keys.contains("jei.l2hostility_tweaks.final_chance"));
    }

    @Test
    void entityConfigTooltipCoversEveryDisplayField() {
        TraitSpawnIndexSnapshot.EntityConfigView config = new TraitSpawnIndexSnapshot.EntityConfigView(
                id("example:zombies"), "{}", 10, 20, 0.2, 1.5, 0.8, 0.6,
                0.1, 5, 100, 4, true);

        List<String> keys = TraitOverviewPresentation.entityConfigTooltipKeys(config);

        assertEquals(List.of(
                "jei.l2hostility_tweaks.source",
                "jei.l2hostility_tweaks.difficulty_range",
                "jei.l2hostility_tweaks.variation",
                "jei.l2hostility_tweaks.scale",
                "jei.l2hostility_tweaks.max_trait_count",
                "jei.l2hostility_tweaks.apply_chance",
                "jei.l2hostility_tweaks.preset_only"), keys);
    }

    @Test
    void formatsDisplayValuesWithoutInventingPoolProbability() {
        assertEquals("50%", TraitOverviewPresentation.formatPercent(0.5));
        assertEquals("12.35%", TraitOverviewPresentation.formatPercent(0.12345));
        assertEquals("jei.l2hostility_tweaks.pool_weight",
                TraitOverviewPresentation.poolTooltipKeys(pool()).get(0));
        assertFalse(TraitOverviewPresentation.poolTooltipKeys(pool()).contains(
                "jei.l2hostility_tweaks.final_chance"));
    }

    @Test
    void formatsEntityConfigLogicalPathInsideDataDirectory() {
        assertEquals("data/example/l2hostility_config/entity/hostile/zombies.json",
                TraitOverviewPresentation.formatEntityConfigPath(id("example:hostile/zombies")));
        assertEquals("-", TraitOverviewPresentation.formatEntityConfigPath(null));
    }

    @Test
    void difficultyRangeUsesMinimumAsClampInsteadOfAddingBase() {
        TraitSpawnIndexSnapshot.EntityConfigView config = new TraitSpawnIndexSnapshot.EntityConfigView(
                id("example:zombies"), "", 10, 20, 0.2, 1.5, 0.8, 0.6,
                0.1, 5, 100, 4, true);

        TraitOverviewPresentation.DifficultyRange range =
                TraitOverviewPresentation.difficultyRange(config);

        assertEquals(10, range.minimum());
        assertEquals(100, range.maximum());
        assertEquals(20, range.base());
    }

    @Test
    void maximumTraitCountDistinguishesDatapackOverrideFromGlobalFallback() {
        TraitSpawnIndexSnapshot.EntityConfigView inherited = new TraitSpawnIndexSnapshot.EntityConfigView(
                id("example:inherited"), "", 0, 0, 0, 0, 1, 1,
                0, 0, 100, -1, false);
        TraitSpawnIndexSnapshot.EntityConfigView overridden = new TraitSpawnIndexSnapshot.EntityConfigView(
                id("example:overridden"), "", 0, 0, 0, 0, 1, 1,
                0, 0, 100, 4, false);

        assertTrue(TraitOverviewPresentation.usesGlobalMaxTraitCount(inherited));
        assertFalse(TraitOverviewPresentation.usesGlobalMaxTraitCount(overridden));
    }

    @Test
    void blockedReasonsAreDistinctAfterSourcesAreHidden() {
        TraitSpawnIndexSnapshot.BlockedTraitView blocked = new TraitSpawnIndexSnapshot.BlockedTraitView(
                id("l2hostility:growth"), id("l2hostility:growth"), List.of(
                new TraitSpawnIndexSnapshot.BlockedContextView(id("example:first"),
                        List.of(TraitBlockReason.PRESET_ONLY, TraitBlockReason.ENTITY_NO_TRAIT)),
                new TraitSpawnIndexSnapshot.BlockedContextView(id("example:second"),
                        List.of(TraitBlockReason.PRESET_ONLY))));

        assertEquals(List.of(
                        "jei.l2hostility_tweaks.block.preset_only",
                        "jei.l2hostility_tweaks.block.no_trait"),
                TraitOverviewPresentation.blockedReasonKeys(blocked));
    }

    @Test
    void compactsConditionsAndMapsRestrictionKeys() {
        assertEquals("{\"type\":\"forge:and\",\"values\":[1,2]}",
                TraitOverviewPresentation.compactConditionJson(" { \"type\" : \"forge:and\", \n \"values\" : [1, 2] } "));
        assertEquals("jei.l2hostility_tweaks.block.preset_only",
                TraitOverviewPresentation.blockReasonKey(TraitBlockReason.PRESET_ONLY));
        assertEquals("jei.l2hostility_tweaks.dynamic.dimension",
                TraitOverviewPresentation.dynamicConstraintKey(new TraitDynamicConstraint("dimension", List.of("minecraft:nether"))));
        assertEquals("-", TraitOverviewPresentation.formatNullableId(null));
        assertEquals("minecraft:zombie", TraitOverviewPresentation.formatNullableId(id("minecraft:zombie")));
    }

    @Test
    void conditionalPagesExposeAStableLocalizedVariantKind() {
        var base = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("goety:apostle"), 0, List.of(), List.of(), List.of(), List.of(), List.of());
        var nbt = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("goety:apostle"), 1,
                List.of(configWithCondition("{\"nbt\":{\"isApollyon\":1}}")),
                List.of(), List.of(), List.of(), List.of());
        var conditional = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("goety:apostle"), 2,
                List.of(configWithCondition("{\"specialConditions\":[\"example:test\"]}")),
                List.of(), List.of(), List.of(), List.of());

        assertEquals("", TraitOverviewPresentation.mobVariantTitleKey(base));
        assertEquals("jei.l2hostility_tweaks.mob_variant.nbt",
                TraitOverviewPresentation.mobVariantTitleKey(nbt));
        assertEquals("jei.l2hostility_tweaks.mob_variant.condition",
                TraitOverviewPresentation.mobVariantTitleKey(conditional));
    }

    @Test
    void configuredJeiDisplayNameReplacesEntityNameAndVariantSuffix() {
        var named = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("goety:apostle"), 1, "使徒（天启形态）",
                List.of(configWithCondition("{\"nbt\":{\"isApollyon\":1}}")),
                List.of(), List.of(), List.of(), List.of());
        var fallback = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("goety:apostle"), 1,
                List.of(configWithCondition("{\"nbt\":{\"isApollyon\":1}}")),
                List.of(), List.of(), List.of(), List.of());

        assertEquals("使徒（天启形态）",
                TraitOverviewPresentation.mobDisplayName(named, "使徒"));
        assertEquals("", TraitOverviewPresentation.mobVariantTitleKey(named));
        assertEquals("使徒", TraitOverviewPresentation.mobDisplayName(fallback, "使徒"));
        assertEquals("jei.l2hostility_tweaks.mob_variant.nbt",
                TraitOverviewPresentation.mobVariantTitleKey(fallback));
    }

    @Test
    void categoryUsesTheSharedRecipeTypeAndFixedPageSize() {
        assertEquals(JeiRuntimeBridge.PAGE_TYPE, TraitOverviewCategory.TYPE);
        assertEquals("l2hostility:teleport", TraitOverviewCategory.ICON_ITEM_ID.toString());
        assertEquals(176, TraitOverviewCategory.WIDTH);
        assertEquals(124, TraitOverviewCategory.HEIGHT);
    }

    @Test
    void categoryUsesTallLeftPreviewAndThreeStackedRightSections() {
        assertEquals(32, TraitOverviewCategory.MOB_CENTER_X);
        assertEquals(4, TraitOverviewCategory.MOB_SLOT_X);
        assertEquals(12, TraitOverviewCategory.MOB_SLOT_Y);
        assertEquals(56, TraitOverviewCategory.MOB_RENDERER_WIDTH);
        assertEquals(72, TraitOverviewCategory.MOB_RENDERER_HEIGHT);
        assertEquals(62, TraitOverviewCategory.EQUIPMENT_SLOT_X);
        assertEquals(132, TraitOverviewCategory.RIGHT_COLUMN_CENTER_X);
        assertEquals(80, TraitOverviewCategory.TRAIT_TITLE_MAX_WIDTH);
        assertEquals(123, TraitOverviewCategory.POOL_SLOT_X);
        assertEquals(12, TraitOverviewCategory.POOL_SLOT_Y);
        assertEquals(123, TraitOverviewCategory.BLOCKED_SLOT_X);
        assertEquals(52, TraitOverviewCategory.BLOCKED_SLOT_Y);
        assertEquals(123, TraitOverviewCategory.PRESET_SLOT_X);
        assertEquals(92, TraitOverviewCategory.PRESET_SLOT_Y);
        assertEquals(4, TraitOverviewCategory.CONFIG_TEXT_X);
        assertEquals(112, TraitOverviewCategory.DIFFICULTY_TEXT_Y);
        assertTrue(TraitOverviewCategory.EQUIPMENT_SLOT_X >=
                TraitOverviewCategory.MOB_SLOT_X + TraitOverviewCategory.MOB_RENDERER_WIDTH);
        assertTrue(TraitOverviewCategory.EQUIPMENT_SLOT_X + 18 < TraitOverviewCategory.POOL_SLOT_X);
    }

    @Test
    void categoryScalesOnlyTextThatExceedsItsPixelBudget() {
        assertEquals(1.0F, TraitOverviewCategory.textScale(40, 80));
        assertEquals(1.0F, TraitOverviewCategory.textScale(80, 80));
        assertEquals(0.5F, TraitOverviewCategory.textScale(160, 80));
    }

    @Test
    void presetSectionVisibilityFollowsRecipeContents() {
        TraitSpawnIndexSnapshot.MobTraitOverview withPreset = overviewWithPresetPoolAndBlocked();
        TraitSpawnIndexSnapshot.MobTraitOverview withoutPreset = new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("minecraft:zombie"), List.of(), List.of(), List.of(), List.of(), List.of());

        assertTrue(TraitOverviewCategory.shouldRenderPresets(withPreset));
        assertFalse(TraitOverviewCategory.shouldRenderPresets(withoutPreset));
    }

    @Test
    void mobPreviewHoverAreaCoversTheWholeCustomRendererAndExcludesItsEdges() {
        assertTrue(TraitOverviewCategory.isInsideMobPreview(4.0, 12.0));
        assertTrue(TraitOverviewCategory.isInsideMobPreview(59.999, 83.999));
        assertFalse(TraitOverviewCategory.isInsideMobPreview(3.999, 12.0));
        assertFalse(TraitOverviewCategory.isInsideMobPreview(60.0, 12.0));
        assertFalse(TraitOverviewCategory.isInsideMobPreview(4.0, 84.0));
    }

    @Test
    void localeSectionLabelsDescribeAvailabilitySemantics() throws IOException {
        assertEquals("可生成词条（%s）", translation("zh_cn", "jei.l2hostility_tweaks.pool"));
        assertEquals("不可生成词条（%s）", translation("zh_cn", "jei.l2hostility_tweaks.blocked"));
        assertEquals("预设词条（%s）", translation("zh_cn", "jei.l2hostility_tweaks.preset"));
        assertEquals("Available traits (%s)", translation("en_us", "jei.l2hostility_tweaks.pool"));
        assertEquals("Unavailable traits (%s)", translation("en_us", "jei.l2hostility_tweaks.blocked"));
        assertEquals("Preset traits (%s)", translation("en_us", "jei.l2hostility_tweaks.preset"));
    }

    private static TraitSpawnIndexSnapshot.MobTraitOverview overviewWithPresetPoolAndBlocked() {
        return new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("minecraft:zombie"),
                List.of(),
                List.of(presetAtHalfChance()),
                List.of(pool()),
                List.of(new TraitSpawnIndexSnapshot.BlockedTraitView(
                        id("l2hostility:growth"), id("l2hostility:growth"),
                        List.of(new TraitSpawnIndexSnapshot.BlockedContextView(
                                id("example:config"), List.of(TraitBlockReason.PRESET_ONLY))))),
                List.of());
    }

    private static TraitSpawnIndexSnapshot.PresetTraitView presetAtHalfChance() {
        return new TraitSpawnIndexSnapshot.PresetTraitView(
                id("l2hostility:adaptive"), id("l2hostility:adaptive"), 1, 2, false,
                0.5, 100, id("minecraft:story/mine_stone"), id("example:zombies"),
                "{ \"type\": \"forge:and\" }", 0,
                List.of(new TraitDynamicConstraint("level", List.of("100"))));
    }

    private static TraitSpawnIndexSnapshot.PresetTraitView preset(
            ResourceLocation traitId, int free, int min, double chance, int level,
            ResourceLocation advancement, String conditionJson, List<TraitDynamicConstraint> constraints) {
        return new TraitSpawnIndexSnapshot.PresetTraitView(
                traitId, traitId, free, min, false, chance, level, advancement,
                id("example:zombies"), conditionJson,
                chance >= 1.0D && level <= 0 && advancement == null ? Math.max(free, min) : 0,
                constraints);
    }

    private static TraitSpawnIndexSnapshot.PoolTraitView pool() {
        return new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility:speedy"), id("l2hostility:speedy"), 37, 10, 2, 5);
    }

    private static TraitSpawnIndexSnapshot.EntityConfigView configWithCondition(String conditionJson) {
        return new TraitSpawnIndexSnapshot.EntityConfigView(
                id("example:apostle"), conditionJson, 0, 20, 0, 0, 1, 1,
                0, 0, 3000, -1, false);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private static String translation(String locale, String key) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/l2hostility_tweaks/lang/" + locale + ".json")))
                .getAsJsonObject().get(key).getAsString();
    }
}
