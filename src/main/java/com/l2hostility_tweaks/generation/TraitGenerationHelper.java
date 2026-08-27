package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.config.L2HConfig.ExclusionGroup;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.stream.Collectors;

public class TraitGenerationHelper {

    public static void applyExclusions(HashMap<MobTrait, Integer> traits, RandomSource random) {
        List<ExclusionGroup> groups = L2HConfig.getExclusionGroups();

        for (ExclusionGroup group : groups) {
            List<String> present = group.traitIds().stream()
                    .filter(id -> hasTraitById(traits, id))
                    .collect(Collectors.toList());

            if (present.size() <= 1) continue;

            String keep;
            if ("roll".equals(group.rule())) {
                keep = present.get(random.nextInt(present.size()));
            } else if ("first".equals(group.rule())) {
                String first = group.traitIds().get(0);
                keep = present.contains(first) ? first : present.get(0);
            } else {
                continue;
            }

            for (String id : present) {
                if (!id.equals(keep)) {
                    removeTraitById(traits, id);
                }
            }
        }
    }

    public static void applyFinalFilters(LivingEntity entity, HashMap<MobTrait, Integer> traits,
                                         int difficulty) {
        if (entity == null || !entity.isAlive() || traits == null || traits.isEmpty()) return;
        Set<String> protectedIds = getDataPackPresetIds(entity);

        if (L2HConfig.COMMON.legendaryEnabled.get()) {
            Set<String> extraLegendaryIds = L2HConfig.getExtraLegendaryIds();
            if (difficulty < L2HConfig.COMMON.legendaryUnlimited.get()) {
                int maxAllowed = L2HConfig.getThreshold(L2HConfig.getLegendaryThresholds(), difficulty);
                List<Map.Entry<MobTrait, Integer>> legendaries = new ArrayList<>();
                for (Map.Entry<MobTrait, Integer> entry : traits.entrySet()) {
                    String id = entry.getKey().getID();
                    boolean isLegendary = entry.getKey() instanceof LegendaryTrait || extraLegendaryIds.contains(id);
                    if (isLegendary && !protectedIds.contains(id) && entry.getValue() > 0) {
                        legendaries.add(entry);
                    }
                }
                legendaries.sort((first, second) -> Integer.compare(second.getValue(), first.getValue()));
                for (int index = maxAllowed; index < legendaries.size(); index++) {
                    traits.remove(legendaries.get(index).getKey());
                }
            }
        }

        if (L2HConfig.COMMON.exclusionEnabled.get()) {
            applyExclusions(traits, entity.getRandom());
        }
    }

    // ==================== Helpers ====================

    private static boolean hasTraitById(HashMap<MobTrait, Integer> traits, String id) {
        for (Map.Entry<MobTrait, Integer> entry : traits.entrySet()) {
            if (entry.getValue() > 0 && entry.getKey().getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static void removeTraitById(HashMap<MobTrait, Integer> traits, String id) {
        traits.entrySet().removeIf(e -> e.getKey().getID().equals(id));
    }

    public static Set<String> getDataPackPresetIds(LivingEntity entity) {
        Set<String> presets = new LinkedHashSet<>();
        try {
            MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
            if (cap == null) return presets;
            EntityConfig.Config cfg = cap.getConfigCache(entity);
            if (cfg == null) return presets;
            for (EntityConfig.TraitBase entry : cfg.traits()) {
                MobTrait t = entry.trait();
                if (t != null) {
                    presets.add(t.getID());
                }
            }
        } catch (Exception ignored) {}
        return presets;
    }
}
