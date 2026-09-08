package com.l2hostility_tweaks.compat.jei;

import com.google.gson.JsonParser;
import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class TraitOverviewPresentation {

    public static Set<ResourceLocation> searchableOutputItemIds(
            TraitSpawnIndexSnapshot.MobTraitOverview overview) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        overview.presets().stream().map(TraitSpawnIndexSnapshot.PresetTraitView::itemId)
                .forEach(ids::add);
        overview.pool().stream().map(TraitSpawnIndexSnapshot.PoolTraitView::itemId)
                .forEach(ids::add);
        return Collections.unmodifiableSet(ids);
    }

    public static Counts counts(TraitSpawnIndexSnapshot.MobTraitOverview overview) {
        return new Counts(overview.configs().size(), overview.presets().size(), overview.pool().size(),
                overview.blocked().size(), overview.dynamicConstraints().size());
    }

    public static List<TraitSpawnIndexSnapshot.PresetTraitView> guaranteedPresets(
            TraitSpawnIndexSnapshot.MobTraitOverview overview) {
        LinkedHashMap<ResourceLocation, TraitSpawnIndexSnapshot.PresetTraitView> guaranteed =
                new LinkedHashMap<>();
        overview.presets().stream().filter(preset -> isGuaranteedPreset(overview, preset))
                .forEach(preset -> guaranteed.merge(preset.traitId(), preset,
                        TraitOverviewPresentation::strongerGuaranteedPreset));
        return List.copyOf(guaranteed.values());
    }

    public static int guaranteedPresetRank(TraitSpawnIndexSnapshot.PresetTraitView preset) {
        return preset.freeRank();
    }

    public static List<TraitSpawnIndexSnapshot.PresetTraitView> presetsForItem(
            TraitSpawnIndexSnapshot.MobTraitOverview overview, ResourceLocation itemId) {
        return overview.presets().stream()
                .filter(preset -> preset.itemId().equals(itemId))
                .toList();
    }

    private static TraitSpawnIndexSnapshot.PresetTraitView strongerGuaranteedPreset(
            TraitSpawnIndexSnapshot.PresetTraitView first,
            TraitSpawnIndexSnapshot.PresetTraitView second) {
        int firstRank = guaranteedPresetRank(first);
        int secondRank = guaranteedPresetRank(second);
        if (firstRank != secondRank) return firstRank > secondRank ? first : second;
        return first.freeRank() >= second.freeRank() ? first : second;
    }

    public static List<ResourceLocation> poolTraitIds(TraitSpawnIndexSnapshot.MobTraitOverview overview) {
        return distinctIds(overview.pool(), TraitSpawnIndexSnapshot.PoolTraitView::traitId);
    }

    public static List<ResourceLocation> blockedTraitIds(TraitSpawnIndexSnapshot.MobTraitOverview overview) {
        return distinctIds(overview.blocked(), TraitSpawnIndexSnapshot.BlockedTraitView::traitId);
    }

    private static boolean isGuaranteedPreset(TraitSpawnIndexSnapshot.MobTraitOverview overview,
                                               TraitSpawnIndexSnapshot.PresetTraitView preset) {
        boolean pageConditionSatisfied = overview.variantIndex() > 0
                && overview.configs().stream().anyMatch(config ->
                Objects.equals(config.conditionJson(), preset.conditionJson()));
        return guaranteedPresetRank(preset) > 0
                && preset.chance() >= 1.0D
                && preset.conditionLevel() <= 0
                && preset.advancementId() == null
                && (preset.conditionJson() == null || preset.conditionJson().isBlank()
                || pageConditionSatisfied)
                && preset.dynamicConstraints().stream()
                .noneMatch(constraint -> constraint.type().equals("runtime_allow"));
    }

    private static <T> List<ResourceLocation> distinctIds(
            List<T> values, Function<T, ResourceLocation> mapper) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        values.stream().map(mapper).forEach(ids::add);
        return List.copyOf(ids);
    }

    public static <T> T cycle(List<T> values, long step) {
        if (values.isEmpty()) return null;
        return values.get(Math.floorMod(step, values.size()));
    }

    public static String formatPercent(double probability) {
        if (!Double.isFinite(probability)) return Double.toString(probability);
        return BigDecimal.valueOf(probability)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    public static String compactConditionJson(String json) {
        if (json == null || json.isBlank()) return "";
        StringBuilder compact = new StringBuilder(json.length());
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char character = json.charAt(i);
            if (quoted) {
                compact.append(character);
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    quoted = false;
                }
            } else if (character == '"') {
                quoted = true;
                compact.append(character);
            } else if (!Character.isWhitespace(character)) {
                compact.append(character);
            }
        }
        return compact.toString();
    }

    public static String formatNullableId(ResourceLocation id) {
        return id == null ? "-" : id.toString();
    }

    public static String formatEntityConfigPath(ResourceLocation id) {
        if (id == null) return "-";
        return "data/" + id.getNamespace() + "/l2hostility_config/entity/" + id.getPath() + ".json";
    }

    public static String mobVariantTitleKey(TraitSpawnIndexSnapshot.MobTraitOverview overview) {
        if (overview.variantIndex() == 0) return "";
        boolean nbtCondition = overview.configs().stream()
                .map(TraitSpawnIndexSnapshot.EntityConfigView::conditionJson)
                .filter(json -> json != null && !json.isBlank())
                .anyMatch(TraitOverviewPresentation::containsNbtCondition);
        return nbtCondition
                ? "jei.l2hostility_tweaks.mob_variant.nbt"
                : "jei.l2hostility_tweaks.mob_variant.condition";
    }

    private static boolean containsNbtCondition(String json) {
        try {
            var parsed = JsonParser.parseString(json);
            return parsed.isJsonObject() && parsed.getAsJsonObject().has("nbt");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static DifficultyRange difficultyRange(TraitSpawnIndexSnapshot.EntityConfigView config) {
        int maximum = config.maxLevel();
        return new DifficultyRange(Math.min(config.minDifficulty(), maximum), maximum, config.baseDifficulty());
    }

    public static boolean usesGlobalMaxTraitCount(TraitSpawnIndexSnapshot.EntityConfigView config) {
        return config.maxTraitCount() <= 0;
    }

    public static List<String> presetTooltipKeys(TraitSpawnIndexSnapshot.PresetTraitView preset) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add("jei.l2hostility_tweaks.preset_chance");
        keys.add("jei.l2hostility_tweaks.free_rank");
        keys.add("jei.l2hostility_tweaks.min_rank");
        keys.add("jei.l2hostility_tweaks.cap");
        keys.add("jei.l2hostility_tweaks.condition_level");
        keys.add("jei.l2hostility_tweaks.advancement");
        keys.add("jei.l2hostility_tweaks.condition");
        return List.copyOf(keys);
    }

    public static List<String> poolTooltipKeys(TraitSpawnIndexSnapshot.PoolTraitView pool) {
        return List.of(
                "jei.l2hostility_tweaks.pool_weight",
                "jei.l2hostility_tweaks.min_level",
                "jei.l2hostility_tweaks.cost",
                "jei.l2hostility_tweaks.max_rank");
    }

    public static List<String> entityConfigTooltipKeys(TraitSpawnIndexSnapshot.EntityConfigView config) {
        return List.of(
                "jei.l2hostility_tweaks.source",
                "jei.l2hostility_tweaks.difficulty_range",
                "jei.l2hostility_tweaks.variation",
                "jei.l2hostility_tweaks.scale",
                "jei.l2hostility_tweaks.max_trait_count",
                "jei.l2hostility_tweaks.apply_chance",
                "jei.l2hostility_tweaks.preset_only");
    }

    public static String blockReasonKey(TraitBlockReason reason) {
        return switch (reason) {
            case ENTITY_CONFIG_BLACKLIST -> "jei.l2hostility_tweaks.block.entity_config";
            case TRAIT_ENTITY_BLACKLIST -> "jei.l2hostility_tweaks.block.trait_blacklist";
            case TRAIT_WHITELIST_MISS -> "jei.l2hostility_tweaks.block.whitelist_miss";
            case TRAIT_GLOBALLY_DISABLED -> "jei.l2hostility_tweaks.block.globally_disabled";
            case ENTITY_NO_TRAIT -> "jei.l2hostility_tweaks.block.no_trait";
            case TRAIT_RUNTIME_REJECTED -> "jei.l2hostility_tweaks.block.runtime_rejected";
            case PRESET_ONLY -> "jei.l2hostility_tweaks.block.preset_only";
            case TWEAKS_DISABLE_RANDOM -> "jei.l2hostility_tweaks.block.disable_random";
            case TWEAKS_DISABLE_ALL -> "jei.l2hostility_tweaks.block.disable_all";
            case TWEAKS_DISABLE_MOB_LEVEL -> "jei.l2hostility_tweaks.block.disable_mob_level";
        };
    }

    public static List<String> blockedReasonKeys(TraitSpawnIndexSnapshot.BlockedTraitView blocked) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        blocked.contexts().forEach(context -> context.reasons().stream()
                .map(TraitOverviewPresentation::blockReasonKey).forEach(keys::add));
        return List.copyOf(keys);
    }

    public static String dynamicConstraintKey(TraitDynamicConstraint constraint) {
        return "jei.l2hostility_tweaks.dynamic." + constraint.type();
    }

    public record Counts(int configs, int presets, int pool, int blocked, int dynamicConstraints) {
    }

    public record DifficultyRange(int minimum, int maximum, int base) {
    }

    private TraitOverviewPresentation() {
    }
}
