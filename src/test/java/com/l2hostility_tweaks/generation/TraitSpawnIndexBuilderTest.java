package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.ConfigInput;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.EntityInput;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.Inputs;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.PresetInput;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.Settings;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.TraitInput;
import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.EntityConfigView;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSpawnIndexBuilderTest {

    @Test
    void mergesPresetPoolAndBlockedTraitsIntoOneMobOverview() {
        ConfigInput config = config("example:zombie", "", view("example:zombie", "", 0.8, 0.6, 0),
                Set.of(), List.of(preset("l2hostility:adaptive", 2, 3, false, 0.5, 200, null)));
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false, List.of(config), List.of())),
                List.of(
                        trait("l2hostility:adaptive", 30, 20, 10, 3, false, Set.of(), Set.of(), false),
                        trait("l2hostility:speedy", 100, 50, 20, 5, false, Set.of(), Set.of(), false),
                        trait("l2hostility:growth", 40, 10, 5, 2, false, Set.of(),
                                Set.of(id("minecraft:skeleton")), false)),
                settings(false, false, false, false, false, List.of()));

        var page = TraitSpawnIndexBuilder.build(4, inputs).mobs().get(0);

        assertEquals(id("minecraft:zombie"), page.entityId());
        assertEquals(1, page.presets().size());
        assertEquals(0.5, page.presets().get(0).chance());
        assertEquals(100, page.pool().stream()
                .filter(entry -> entry.traitId().equals(id("l2hostility:speedy")))
                .findFirst().orElseThrow().weight());
        assertEquals(TraitBlockReason.TRAIT_WHITELIST_MISS,
                page.blocked().get(0).contexts().get(0).reasons().get(0));
    }

    @Test
    void activeBaseContextAloneControlsTheRandomPool() {
        TraitInput speedy = trait("l2hostility:speedy", 100, 50, 20, 5,
                false, Set.of(), Set.of(), false);
        ConfigInput blocked = config("example:blocked", "", view("example:blocked", "", 1, 1, 0),
                Set.of(speedy.traitId()), List.of());
        ConfigInput allowed = config("example:allowed", "", view("example:allowed", "", 1, 1, 0),
                Set.of(), List.of());
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false, List.of(blocked, allowed), List.of())),
                List.of(speedy), settings(false, false, false, false, false, List.of()));

        var page = TraitSpawnIndexBuilder.build(1, inputs).mobs().get(0);

        assertTrue(page.pool().stream().anyMatch(entry -> entry.traitId().getPath().equals("speedy")));
        assertTrue(page.blocked().stream().noneMatch(entry -> entry.traitId().getPath().equals("speedy")));
    }

    @Test
    void usesOnlyTheLastBaseConfigAppliedByL2Hostility() {
        TraitInput speedy = trait("l2hostility:speedy", 100, 50, 20, 5,
                false, Set.of(), Set.of(), false);
        ConfigInput replaced = config("example:replaced", "", view("example:replaced", "", 1, 1, 0),
                Set.of(), List.of());
        ConfigInput active = config("example:active", "", view("example:active", "", 1, 1, 0),
                Set.of(speedy.traitId()), List.of());
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false, List.of(replaced, active), List.of())),
                List.of(speedy), settings(false, false, false, false, false, List.of()));

        var snapshot = TraitSpawnIndexBuilder.build(1, inputs);

        assertEquals(1, snapshot.mobs().size());
        assertEquals(List.of(id("example:active")), snapshot.mobs().get(0).configs().stream()
                .map(EntityConfigView::sourceId).toList());
        assertTrue(snapshot.mobs().get(0).pool().isEmpty());
    }

    @Test
    void createsOneIndependentPageForEachConditionalConfigEntry() {
        TraitInput adaptive = trait("l2hostility:adaptive", 20, 2, 2, 2,
                false, Set.of(), Set.of(), false);
        TraitInput speedy = trait("l2hostility:speedy", 100, 50, 20, 5,
                false, Set.of(), Set.of(), false);
        ConfigInput base = config("example:base", "", view("example:base", "", 1, 1, 0),
                Set.of(), List.of(preset("l2hostility:adaptive", 1, 1, false, 1, 0, null)));
        String nbt = "{\"nbt\":{\"isApollyon\":1}}";
        ConfigInput conditional = config("example:nbt", nbt, view("example:nbt", nbt, 1, 1, 0),
                Set.of(), List.of(preset("l2hostility:speedy", 2, 2, false, 1, 0, null)));
        Inputs inputs = inputs(
                List.of(entity("goety:apostle", false, List.of(base),
                        List.of(conditional, conditional))),
                List.of(adaptive, speedy), settings(false, false, false, false, false, List.of()));

        var pages = TraitSpawnIndexBuilder.build(1, inputs).mobs();

        assertEquals(3, pages.size());
        assertEquals(List.of(id("example:base")), pages.get(0).configs().stream()
                .map(EntityConfigView::sourceId).toList());
        assertEquals(List.of(id("l2hostility:adaptive")), pages.get(0).presets().stream()
                .map(entry -> entry.traitId()).toList());
        assertEquals(List.of(id("example:nbt")), pages.get(1).configs().stream()
                .map(EntityConfigView::sourceId).toList());
        assertEquals(List.of(id("l2hostility:speedy")), pages.get(1).presets().stream()
                .map(entry -> entry.traitId()).toList());
        assertEquals(1, pages.get(1).variantIndex());
        assertEquals(2, pages.get(2).variantIndex());
    }

    @Test
    void blockedTraitReportsOnlyTheActiveBaseContext() {
        TraitInput speedy = trait("l2hostility:speedy", 100, 50, 20, 5,
                false, Set.of(), Set.of(), false);
        ConfigInput first = config("example:first", "", view("example:first", "", 1, 1, 0),
                Set.of(speedy.traitId()), List.of());
        ConfigInput second = config("example:second", "", view("example:second", "", 1, 1, 0),
                Set.of(speedy.traitId()), List.of());
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false, List.of(first, second), List.of())),
                List.of(speedy), settings(false, false, false, false, false, List.of()));

        var page = TraitSpawnIndexBuilder.build(1, inputs).mobs().get(0);

        assertTrue(page.pool().isEmpty());
        assertEquals(1, page.blocked().get(0).contexts().size());
        assertEquals(id("example:second"), page.blocked().get(0).contexts().get(0).sourceId());
    }

    @Test
    void keepsSpecialConditionPresetsOnTheirOwnPage() {
        TraitInput speedy = trait("l2hostility:speedy", 100, 50, 20, 5,
                false, Set.of(), Set.of(), false);
        ConfigInput base = config("example:base", "", view("example:base", "", 1, 1, 0),
                Set.of(), List.of());
        String condition = "{\"nbt\":{\"Health\":100}}";
        ConfigInput conditional = config("example:conditional", condition,
                view("example:conditional", condition, 1, 1, 0), Set.of(),
                List.of(preset("l2hostility:speedy", 1, 1, false, 1, 0, null)));
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false, List.of(base), List.of(conditional))),
                List.of(speedy), settings(false, false, false, false, false, List.of()));

        var pages = TraitSpawnIndexBuilder.build(1, inputs).mobs();
        var page = pages.get(1);

        assertEquals(2, pages.size());
        assertEquals(1, page.presets().size());
        assertEquals(condition, page.presets().get(0).conditionJson());
        assertEquals(1, page.pool().size());
        assertTrue(page.dynamicConstraints().stream().anyMatch(entry -> entry.type().equals("condition")));
    }

    @Test
    void conditionalConfigUsesItsOwnRandomPoolContext() {
        TraitInput speedy = trait("l2hostility:speedy", 100, 50, 20, 5,
                false, Set.of(), Set.of(), false);
        ConfigInput blockedBase = config("example:base", "", view("example:base", "", 1, 1, 0),
                Set.of(speedy.traitId()), List.of());
        ConfigInput allowingConditional = config("example:conditional", "{\"nbt\":{}}",
                view("example:conditional", "{\"nbt\":{}}", 1, 1, 0), Set.of(), List.of());
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false,
                        List.of(blockedBase), List.of(allowingConditional))),
                List.of(speedy), settings(false, false, false, false, false, List.of()));

        var pages = TraitSpawnIndexBuilder.build(1, inputs).mobs();
        var basePage = pages.get(0);
        var conditionalPage = pages.get(1);

        assertTrue(basePage.pool().isEmpty());
        assertEquals(id("l2hostility:speedy"), basePage.blocked().get(0).traitId());
        assertEquals(List.of(id("l2hostility:speedy")), conditionalPage.pool().stream()
                .map(entry -> entry.traitId()).toList());
    }

    @Test
    void sortsEntitiesAndTraitsDeterministicallyAndDeduplicatesOnlyCompletePresetTuples() {
        TraitInput speedy = trait("l2hostility:speedy", 10, 1, 1, 1,
                false, Set.of(), Set.of(), false);
        TraitInput adaptive = trait("l2hostility:adaptive", 20, 2, 2, 2,
                false, Set.of(), Set.of(), false);
        PresetInput first = preset("l2hostility:adaptive", 1, 2, false, 1, 0, null);
        PresetInput distinctTuple = preset("l2hostility:adaptive", 2, 2, false, 1, 0, null);
        ConfigInput config = config("example:source", "", view("example:source", "", 1, 1, 0),
                Set.of(), List.of(first, first, distinctTuple));
        Inputs inputs = inputs(
                List.of(
                        entity("minecraft:zombie", false, List.of(config), List.of()),
                        entity("minecraft:allay", false, List.of(config), List.of())),
                List.of(speedy, adaptive), settings(false, false, false, false, false, List.of()));

        var snapshot = TraitSpawnIndexBuilder.build(7, inputs);

        assertEquals(List.of(id("minecraft:allay"), id("minecraft:zombie")),
                snapshot.mobs().stream().map(page -> page.entityId()).toList());
        assertEquals(List.of(id("l2hostility:adaptive"), id("l2hostility:speedy")),
                snapshot.mobs().get(0).pool().stream().map(entry -> entry.traitId()).toList());
        assertEquals(List.of(1, 2), snapshot.mobs().get(0).presets().stream()
                .map(entry -> entry.freeRank()).toList());
    }

    @Test
    void defaultsUnconditionalPresetChanceAndPreservesConfigChances() {
        PresetInput preset = new PresetInput(id("l2hostility:adaptive"), 1, 2, false);
        EntityConfigView view = view("example:source", "", 0.25, 0.75, 0);
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false,
                        List.of(config("example:source", "", view, Set.of(), List.of(preset))), List.of())),
                List.of(trait("l2hostility:adaptive", 20, 2, 2, 2,
                        false, Set.of(), Set.of(), false)),
                settings(false, false, false, false, false, List.of()));

        var page = TraitSpawnIndexBuilder.build(1, inputs).mobs().get(0);

        assertEquals(1.0, page.presets().get(0).chance());
        assertEquals(0.25, page.configs().get(0).applyChance());
        assertEquals(0.75, page.configs().get(0).traitChance());
    }

    @Test
    void reportsEveryRuntimeGenerationConstraint() {
        TraitInput adaptive = trait("l2hostility:adaptive", 20, 2, 2, 2,
                false, Set.of(), Set.of(), true);
        ConfigInput config = config("example:source", "", view("example:source", "", 1, 1, 0.4),
                Set.of(), List.of());
        Inputs inputs = inputs(
                List.of(entity("minecraft:zombie", false, List.of(config), List.of())),
                List.of(adaptive), settings(false, false, false, true, true,
                        List.of(List.of(id("l2hostility:adaptive"), id("l2hostility:speedy")))));

        var constraints = TraitSpawnIndexBuilder.build(1, inputs).mobs().get(0).dynamicConstraints();

        assertEquals(Set.of("runtime_allow", "legendary_limit", "level_cap", "exclusion", "budget", "suppression"),
                constraints.stream().map(entry -> entry.type()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void usesImplicitBaseContextForEntitiesWithoutEntityConfig() {
        Inputs inputs = inputs(
                List.of(entity("example:living", false, List.of(), List.of())),
                List.of(trait("l2hostility:speedy", 100, 50, 20, 5,
                        false, Set.of(), Set.of(), false)),
                settings(false, false, false, false, false, List.of()));

        var page = TraitSpawnIndexBuilder.build(1, inputs).mobs().get(0);

        assertEquals(1, page.pool().size());
        assertTrue(page.blocked().isEmpty());
    }

    @Test
    void runtimeRejectedTraitIsBlockedFromRandomAndPresetPools() {
        TraitInput master = trait("l2hostility:master", 200, 50, 50, 1,
                false, Set.of(), Set.of(), true);
        TraitInput speedy = trait("l2hostility:speedy", 100, 10, 10, 3,
                false, Set.of(), Set.of(), false);
        ConfigInput config = config("example:zombie", "", view("example:zombie", "", 1, 1, 0),
                Set.of(), List.of(preset("l2hostility:master", 1, 1, false, 1, 0, null)));
        EntityInput zombie = new EntityInput(id("minecraft:zombie"), false,
                List.of(config), List.of(), Set.of(master.traitId()));

        var page = TraitSpawnIndexBuilder.build(1,
                inputs(List.of(zombie), List.of(master, speedy),
                        settings(false, false, false, false, false, List.of())))
                .mobs().get(0);

        assertEquals(List.of(id("l2hostility:speedy")),
                page.pool().stream().map(entry -> entry.traitId()).toList());
        assertTrue(page.presets().isEmpty());
        assertEquals(List.of(TraitBlockReason.TRAIT_RUNTIME_REJECTED),
                page.blocked().get(0).contexts().get(0).reasons());
    }

    private static Inputs inputs(List<EntityInput> entities, List<TraitInput> traits, Settings settings) {
        return new Inputs(entities, traits, settings);
    }

    private static EntityInput entity(String id, boolean noTrait,
                                      List<ConfigInput> base, List<ConfigInput> conditional) {
        return new EntityInput(id(id), noTrait, base, conditional);
    }

    private static ConfigInput config(String sourceId, String conditionJson, EntityConfigView view,
                                      Set<ResourceLocation> blacklist, List<PresetInput> presets) {
        return new ConfigInput(id(sourceId), conditionJson, view, blacklist, presets);
    }

    private static EntityConfigView view(String sourceId, String conditionJson,
                                         double applyChance, double traitChance, double suppression) {
        return new EntityConfigView(id(sourceId), conditionJson,
                1, 2, 0.5, 1.5, applyChance, traitChance, suppression, 3, 100, 4, false);
    }

    private static PresetInput preset(String traitId, int freeRank, int minRank, boolean cap,
                                      double chance, int conditionLevel, ResourceLocation advancementId) {
        return new PresetInput(id(traitId), freeRank, minRank, cap, chance, conditionLevel, advancementId);
    }

    private static TraitInput trait(String traitId, int weight, int minLevel, int cost, int maxRank,
                                    boolean globallyDisabled, Set<ResourceLocation> entityBlacklist,
                                    Set<ResourceLocation> entityWhitelist, boolean runtimeAllowOverride) {
        ResourceLocation id = id(traitId);
        return new TraitInput(id, id, weight, minLevel, cost, maxRank, globallyDisabled,
                entityBlacklist, entityWhitelist, runtimeAllowOverride);
    }

    private static Settings settings(boolean disableRandom, boolean disableAll, boolean disableMobLevel,
                                     boolean legendaryLimited, boolean levelCapEnabled,
                                     List<List<ResourceLocation>> exclusions) {
        return new Settings(disableRandom, disableAll, disableMobLevel,
                legendaryLimited, levelCapEnabled, exclusions);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
