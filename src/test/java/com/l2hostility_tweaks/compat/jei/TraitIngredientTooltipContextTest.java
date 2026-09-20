package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TraitIngredientTooltipContextTest {

    @Test
    void poolTooltipUsesEffectiveDatapackMaximumRank() {
        var page = page();
        var context = new TraitIngredientTooltipContext(
                TraitIngredientTooltipContext.Section.POOL, page, id("l2hostility:speedy"));

        List<Component> lines = context.tooltipLines();

        assertEquals(List.of("jei.l2hostility_tweaks.max_rank"), translationKeys(lines));
        assertEquals(7, translation(lines.get(0)).getArgs()[0]);
    }

    @Test
    void blockedTooltipListsEveryDistinctReason() {
        var context = new TraitIngredientTooltipContext(
                TraitIngredientTooltipContext.Section.BLOCKED, page(), id("l2hostility:growth"));

        assertEquals(List.of(
                        "jei.l2hostility_tweaks.blocked_reason_header",
                        "jei.l2hostility_tweaks.block.preset_only",
                        "jei.l2hostility_tweaks.block.no_trait"),
                translationKeys(context.tooltipLines()));
    }

    @Test
    void presetTooltipKeepsMultipleEntriesInSnapshotOrder() {
        var context = new TraitIngredientTooltipContext(
                TraitIngredientTooltipContext.Section.PRESET, page(), id("l2hostility:adaptive"));

        List<String> keys = translationKeys(context.tooltipLines());

        assertEquals(2, keys.stream()
                .filter("jei.l2hostility_tweaks.preset_entry"::equals).count());
        assertEquals(List.of(1, 3), context.overview().presets().stream()
                .map(TraitSpawnIndexSnapshot.PresetTraitView::freeRank).toList());
        assertEquals("jei.l2hostility_tweaks.preset_details_header", keys.get(0));
    }

    private static TraitSpawnIndexSnapshot.MobTraitOverview page() {
        ResourceLocation adaptive = id("l2hostility:adaptive");
        ResourceLocation growth = id("l2hostility:growth");
        return new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("minecraft:zombie"),
                List.of(),
                List.of(
                        preset(adaptive, 1, 2, false, 0.5, 0, null, ""),
                        preset(adaptive, 3, 4, true, 1.0, 100,
                                id("minecraft:story/mine_stone"), "{\"nbt\":{}}")),
                List.of(new TraitSpawnIndexSnapshot.PoolTraitView(
                        id("l2hostility:speedy"), id("l2hostility:speedy"), 20, 10, 2, 7)),
                List.of(new TraitSpawnIndexSnapshot.BlockedTraitView(growth, growth, List.of(
                        new TraitSpawnIndexSnapshot.BlockedContextView(id("example:first"),
                                List.of(TraitBlockReason.PRESET_ONLY, TraitBlockReason.ENTITY_NO_TRAIT)),
                        new TraitSpawnIndexSnapshot.BlockedContextView(id("example:second"),
                                List.of(TraitBlockReason.PRESET_ONLY))))),
                List.of());
    }

    private static TraitSpawnIndexSnapshot.PresetTraitView preset(
            ResourceLocation traitId, int freeRank, int minRank, boolean cap,
            double chance, int conditionLevel, ResourceLocation advancementId, String conditionJson) {
        return new TraitSpawnIndexSnapshot.PresetTraitView(
                traitId, traitId, freeRank, minRank, cap, chance, conditionLevel,
                advancementId, id("example:zombies"), conditionJson, 0, List.of());
    }

    private static List<String> translationKeys(List<Component> components) {
        return components.stream().map(TraitIngredientTooltipContextTest::translation)
                .map(TranslatableContents::getKey).toList();
    }

    private static TranslatableContents translation(Component component) {
        return assertInstanceOf(TranslatableContents.class, component.getContents());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
