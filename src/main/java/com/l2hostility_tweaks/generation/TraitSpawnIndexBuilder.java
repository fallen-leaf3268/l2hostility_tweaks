package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.BlockedContextView;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.BlockedTraitView;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.EntityConfigView;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobTraitOverview;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.PoolTraitView;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.PresetTraitView;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TraitSpawnIndexBuilder {

    private static final Comparator<ResourceLocation> ID_ORDER = Comparator.comparing(ResourceLocation::toString);
    private static final Comparator<ResourceLocation> NULLABLE_ID_ORDER = Comparator.nullsFirst(ID_ORDER);

    public static TraitSpawnIndexSnapshot build(long revision, Inputs inputs) {
        Objects.requireNonNull(inputs);
        Map<ResourceLocation, TraitInput> traitsById = new HashMap<>();
        for (TraitInput trait : inputs.traits()) traitsById.put(trait.traitId(), trait);

        List<TraitInput> traits = inputs.traits().stream()
                .sorted(Comparator.comparing(TraitInput::traitId, ID_ORDER))
                .toList();
        List<MobTraitOverview> mobs = new ArrayList<>();
        int warnings = 0;
        for (EntityInput entity : inputs.entities().stream()
                .sorted(Comparator.comparing(EntityInput::id, ID_ORDER)).toList()) {
            ConfigInput baseConfig = entity.baseConfigs().isEmpty()
                    ? null : entity.baseConfigs().get(entity.baseConfigs().size() - 1);
            int variantIndex = 0;
            BuildResult result = buildEntity(
                    entity, baseConfig, variantIndex, traits, traitsById, inputs.settings());
            mobs.add(result.overview());
            warnings += result.warningCount();
            for (ConfigInput conditional : entity.conditionalConfigs()) {
                result = buildEntity(
                        entity, conditional, ++variantIndex, traits, traitsById, inputs.settings());
                mobs.add(result.overview());
                warnings += result.warningCount();
            }
        }
        return new TraitSpawnIndexSnapshot(revision, mobs, warnings);
    }

    private static BuildResult buildEntity(EntityInput entity, ConfigInput config, int variantIndex,
                                           List<TraitInput> traits,
                                           Map<ResourceLocation, TraitInput> traitsById,
                                           Settings settings) {
        List<PoolTraitView> pool = new ArrayList<>();
        List<BlockedTraitView> blocked = new ArrayList<>();

        for (TraitInput trait : traits) {
            List<BlockedContextView> contexts = new ArrayList<>();
            List<TraitBlockReason> reasons = TraitRuleClassifier.classify(new TraitRuleClassifier.Context(
                    config != null && config.blacklist().contains(trait.traitId()),
                    trait.entityBlacklist().contains(entity.id()),
                    !trait.entityWhitelist().isEmpty() && !trait.entityWhitelist().contains(entity.id()),
                    trait.globallyDisabled(), entity.noTrait(),
                    variantIndex == 0 && entity.runtimeRejectedTraits().contains(trait.traitId()), false,
                    config != null && config.view().presetTraitsOnly(),
                    settings.disableRandom(), settings.disableAll(), settings.disableMobLevel()));
            if (reasons.isEmpty()) {
                pool.add(new PoolTraitView(trait.traitId(), trait.itemId(), trait.weight(),
                        trait.minLevel(), trait.cost(), trait.maxRank()));
            } else {
                contexts.add(new BlockedContextView(config == null ? null : config.sourceId(), reasons));
                blocked.add(new BlockedTraitView(trait.traitId(), trait.itemId(), contexts));
            }
        }

        LinkedHashSet<PresetTraitView> presetSet = new LinkedHashSet<>();
        int warnings = 0;
        if (config != null) {
            for (PresetInput preset : config.presets()) {
                TraitInput trait = traitsById.get(preset.traitId());
                if (trait == null) {
                    warnings++;
                    continue;
                }
                List<TraitBlockReason> reasons = TraitRuleClassifier.classify(new TraitRuleClassifier.Context(
                        config.blacklist().contains(trait.traitId()),
                        trait.entityBlacklist().contains(entity.id()),
                        !trait.entityWhitelist().isEmpty() && !trait.entityWhitelist().contains(entity.id()),
                        trait.globallyDisabled(), entity.noTrait(),
                        variantIndex == 0 && entity.runtimeRejectedTraits().contains(trait.traitId()), true,
                        config.view().presetTraitsOnly(),
                        settings.disableRandom(), settings.disableAll(), settings.disableMobLevel()));
                if (!reasons.isEmpty()) continue;
                presetSet.add(new PresetTraitView(
                        preset.traitId(), trait.itemId(), preset.freeRank(), preset.minRank(), preset.cap(),
                        preset.chance(), preset.conditionLevel(), preset.advancementId(), config.sourceId(),
                        config.conditionJson(), presetConstraints(preset, config, trait)));
            }
        }

        List<PresetTraitView> presets = presetSet.stream().sorted(presetOrder()).toList();
        pool.sort(Comparator.comparing(PoolTraitView::traitId, ID_ORDER));
        blocked.sort(Comparator.comparing(BlockedTraitView::traitId, ID_ORDER));
        List<EntityConfigView> configs = config == null ? List.of() : List.of(config.view());
        List<TraitDynamicConstraint> dynamicConstraints = overviewConstraints(traits, settings, config);
        return new BuildResult(new MobTraitOverview(
                entity.id(), variantIndex, configs, presets, pool, blocked, dynamicConstraints), warnings);
    }

    private static List<TraitDynamicConstraint> presetConstraints(PresetInput preset, ConfigInput config,
                                                                  TraitInput trait) {
        List<TraitDynamicConstraint> constraints = new ArrayList<>();
        if (trait.runtimeAllowOverride()) {
            constraints.add(constraint("runtime_allow", trait.traitId().toString()));
        }
        if (preset.conditionLevel() > 0) {
            constraints.add(constraint("condition_level", Integer.toString(preset.conditionLevel())));
        }
        if (preset.advancementId() != null) {
            constraints.add(constraint("advancement", preset.advancementId().toString()));
        }
        if (!config.conditionJson().isBlank()) {
            constraints.add(constraint("condition", config.conditionJson()));
        }
        constraints.sort(dynamicOrder());
        return List.copyOf(constraints);
    }

    private static List<TraitDynamicConstraint> overviewConstraints(List<TraitInput> traits,
                                                                     Settings settings,
                                                                     ConfigInput config) {
        LinkedHashSet<TraitDynamicConstraint> constraints = new LinkedHashSet<>();
        constraints.add(constraint("budget"));
        if (settings.legendaryLimited()) constraints.add(constraint("legendary_limit"));
        if (settings.levelCapEnabled()) constraints.add(constraint("level_cap"));
        for (List<ResourceLocation> group : settings.exclusions()) {
            constraints.add(new TraitDynamicConstraint("exclusion",
                    group.stream().sorted(ID_ORDER).map(ResourceLocation::toString).toList()));
        }
        for (TraitInput trait : traits) {
            if (trait.runtimeAllowOverride()) {
                constraints.add(constraint("runtime_allow", trait.traitId().toString()));
            }
        }
        if (config != null && config.view().suppression() != 0) {
            constraints.add(constraint("suppression", source(config),
                    Double.toString(config.view().suppression())));
        }
        if (config != null && !config.conditionJson().isBlank()) {
            constraints.add(constraint("condition", source(config), config.conditionJson()));
        }
        return constraints.stream().sorted(dynamicOrder()).toList();
    }

    private static String source(ConfigInput config) {
        return config.sourceId() == null ? "" : config.sourceId().toString();
    }

    private static TraitDynamicConstraint constraint(String type, String... arguments) {
        return new TraitDynamicConstraint(type, List.of(arguments));
    }

    private static Comparator<PresetTraitView> presetOrder() {
        return Comparator.comparing(PresetTraitView::traitId, ID_ORDER)
                .thenComparingInt(PresetTraitView::freeRank)
                .thenComparingInt(PresetTraitView::minRank)
                .thenComparing(PresetTraitView::cap)
                .thenComparingDouble(PresetTraitView::chance)
                .thenComparingInt(PresetTraitView::conditionLevel)
                .thenComparing(PresetTraitView::advancementId, NULLABLE_ID_ORDER)
                .thenComparing(PresetTraitView::sourceId, NULLABLE_ID_ORDER)
                .thenComparing(PresetTraitView::conditionJson, Comparator.nullsFirst(String::compareTo));
    }

    private static Comparator<TraitDynamicConstraint> dynamicOrder() {
        return Comparator.comparing(TraitDynamicConstraint::type)
                .thenComparing(value -> String.join("\u0000", value.arguments()));
    }

    private record BuildResult(MobTraitOverview overview, int warningCount) {}

    public record Inputs(List<EntityInput> entities, List<TraitInput> traits, Settings settings) {
        public Inputs {
            entities = List.copyOf(entities);
            traits = List.copyOf(traits);
            Objects.requireNonNull(settings);
        }
    }

    public record EntityInput(ResourceLocation id, boolean noTrait, List<ConfigInput> baseConfigs,
                              List<ConfigInput> conditionalConfigs,
                              Set<ResourceLocation> runtimeRejectedTraits) {
        public EntityInput {
            Objects.requireNonNull(id);
            baseConfigs = List.copyOf(baseConfigs);
            conditionalConfigs = List.copyOf(conditionalConfigs);
            runtimeRejectedTraits = Set.copyOf(runtimeRejectedTraits);
        }

        public EntityInput(ResourceLocation id, boolean noTrait, List<ConfigInput> baseConfigs,
                           List<ConfigInput> conditionalConfigs) {
            this(id, noTrait, baseConfigs, conditionalConfigs, Set.of());
        }
    }

    public record ConfigInput(ResourceLocation sourceId, String conditionJson,
                              EntityConfigView view, Set<ResourceLocation> blacklist,
                              List<PresetInput> presets) {
        public ConfigInput {
            conditionJson = conditionJson == null ? "" : conditionJson;
            Objects.requireNonNull(view);
            blacklist = Set.copyOf(blacklist);
            presets = List.copyOf(presets);
        }
    }

    public record PresetInput(ResourceLocation traitId, int freeRank, int minRank, boolean cap,
                              double chance, int conditionLevel, ResourceLocation advancementId) {
        public PresetInput {
            Objects.requireNonNull(traitId);
            if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
                throw new IllegalArgumentException("chance must be finite and between 0 and 1");
            }
        }

        public PresetInput(ResourceLocation traitId, int freeRank, int minRank, boolean cap) {
            this(traitId, freeRank, minRank, cap, 1.0, 0, null);
        }
    }

    public record TraitInput(ResourceLocation traitId, ResourceLocation itemId, int weight,
                             int minLevel, int cost, int maxRank, boolean globallyDisabled,
                             Set<ResourceLocation> entityBlacklist, Set<ResourceLocation> entityWhitelist,
                             boolean runtimeAllowOverride) {
        public TraitInput {
            Objects.requireNonNull(traitId);
            Objects.requireNonNull(itemId);
            entityBlacklist = Set.copyOf(entityBlacklist);
            entityWhitelist = Set.copyOf(entityWhitelist);
        }
    }

    public record Settings(boolean disableRandom, boolean disableAll, boolean disableMobLevel,
                           boolean legendaryLimited, boolean levelCapEnabled,
                           List<List<ResourceLocation>> exclusions) {
        public Settings {
            exclusions = exclusions.stream().map(List::copyOf).toList();
        }
    }

    private TraitSpawnIndexBuilder() {}
}
