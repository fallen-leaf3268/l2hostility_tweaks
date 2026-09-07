package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public static List<String> presetTooltipKeys(TraitSpawnIndexSnapshot.PresetTraitView preset) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add("jei.l2hostility_tweaks.preset_chance");
        keys.add("jei.l2hostility_tweaks.free_rank");
        keys.add("jei.l2hostility_tweaks.min_rank");
        keys.add("jei.l2hostility_tweaks.cap");
        keys.add("jei.l2hostility_tweaks.condition_level");
        keys.add("jei.l2hostility_tweaks.advancement");
        keys.add("jei.l2hostility_tweaks.source");
        keys.add("jei.l2hostility_tweaks.condition");
        for (TraitDynamicConstraint constraint : preset.dynamicConstraints()) {
            keys.add(dynamicConstraintKey(constraint));
        }
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
                "jei.l2hostility_tweaks.difficulty",
                "jei.l2hostility_tweaks.variation",
                "jei.l2hostility_tweaks.scale",
                "jei.l2hostility_tweaks.apply_chance",
                "jei.l2hostility_tweaks.trait_chance",
                "jei.l2hostility_tweaks.suppression",
                "jei.l2hostility_tweaks.min_spawn_level",
                "jei.l2hostility_tweaks.max_level",
                "jei.l2hostility_tweaks.max_trait_count",
                "jei.l2hostility_tweaks.preset_only",
                "jei.l2hostility_tweaks.condition");
    }

    public static String blockReasonKey(TraitBlockReason reason) {
        return switch (reason) {
            case ENTITY_CONFIG_BLACKLIST -> "jei.l2hostility_tweaks.block.entity_config";
            case TRAIT_ENTITY_BLACKLIST -> "jei.l2hostility_tweaks.block.trait_blacklist";
            case TRAIT_WHITELIST_MISS -> "jei.l2hostility_tweaks.block.whitelist_miss";
            case TRAIT_GLOBALLY_DISABLED -> "jei.l2hostility_tweaks.block.globally_disabled";
            case ENTITY_NO_TRAIT -> "jei.l2hostility_tweaks.block.no_trait";
            case PRESET_ONLY -> "jei.l2hostility_tweaks.block.preset_only";
            case TWEAKS_DISABLE_RANDOM -> "jei.l2hostility_tweaks.block.disable_random";
            case TWEAKS_DISABLE_ALL -> "jei.l2hostility_tweaks.block.disable_all";
            case TWEAKS_DISABLE_MOB_LEVEL -> "jei.l2hostility_tweaks.block.disable_mob_level";
        };
    }

    public static String dynamicConstraintKey(TraitDynamicConstraint constraint) {
        return "jei.l2hostility_tweaks.dynamic." + constraint.type();
    }

    public record Counts(int configs, int presets, int pool, int blocked, int dynamicConstraints) {
    }

    private TraitOverviewPresentation() {
    }
}
