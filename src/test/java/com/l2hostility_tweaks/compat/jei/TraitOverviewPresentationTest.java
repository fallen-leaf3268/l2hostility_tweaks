package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

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
    void presetTooltipLabelsChanceAsDatapackProbability() {
        List<String> keys = TraitOverviewPresentation.presetTooltipKeys(presetAtHalfChance());

        assertTrue(keys.contains("jei.l2hostility_tweaks.preset_chance"));
        assertTrue(keys.contains("jei.l2hostility_tweaks.source"));
        assertTrue(keys.contains("jei.l2hostility_tweaks.condition"));
        assertTrue(keys.contains("jei.l2hostility_tweaks.dynamic.level"));
        assertFalse(keys.contains("jei.l2hostility_tweaks.final_chance"));
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
    void compactsConditionsAndMapsRestrictionKeys() {
        assertEquals("{\"type\":\"forge:and\",\"values\":[1,2]}",
                TraitOverviewPresentation.compactConditionJson(" { \"type\" : \"forge:and\", \n \"values\" : [1, 2] } "));
        assertEquals("jei.l2hostility_tweaks.block.preset_only",
                TraitOverviewPresentation.blockReasonKey(TraitBlockReason.PRESET_ONLY));
        assertEquals("jei.l2hostility_tweaks.dynamic.dimension",
                TraitOverviewPresentation.dynamicConstraintKey(new TraitDynamicConstraint("dimension", List.of("minecraft:nether"))));
    }

    @Test
    void categoryUsesTheSharedRecipeTypeAndFixedPageSize() {
        assertEquals(JeiRuntimeBridge.PAGE_TYPE, TraitOverviewCategory.TYPE);
        assertEquals(176, TraitOverviewCategory.WIDTH);
        assertEquals(124, TraitOverviewCategory.HEIGHT);
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
                "{ \"type\": \"forge:and\" }",
                List.of(new TraitDynamicConstraint("level", List.of("100"))));
    }

    private static TraitSpawnIndexSnapshot.PoolTraitView pool() {
        return new TraitSpawnIndexSnapshot.PoolTraitView(
                id("l2hostility:speedy"), id("l2hostility:speedy"), 37, 10, 2, 5);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
