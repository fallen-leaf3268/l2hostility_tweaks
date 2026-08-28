package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.config.L2HConfig.ExclusionGroup;
import dev.xkmc.l2hostility.init.L2Hostility;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TraitGenerationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:trait_generation");
    private static final long WARNING_INTERVAL_NANOS = 60_000_000_000L;
    private static final AtomicLong NEXT_WARNING_NANOS = new AtomicLong();
    private static final ResourceLocation NBT_CONDITION_ID =
            new ResourceLocation("l2hostility_tweaks", "nbt");

    public interface PresetState {
        Set<String> l2fix$getAppliedPresetIds();
    }

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
                                         int difficulty, Set<String> protectedIds) {
        if (entity == null || !entity.isAlive() || traits == null || traits.isEmpty()) return;
        if (protectedIds == null) protectedIds = Collections.emptySet();

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

    public static List<EntityConfig.TraitBase> selectActiveNbtPresets(
            LivingEntity entity, int difficulty, MobDifficultyCollector collector) {
        List<EntityConfig.TraitBase> nbtPresets = new ArrayList<>();
        if (entity == null) return List.of();

        try {
            EntityConfig merged = (EntityConfig) L2Hostility.ENTITY.getMerged();
            EntityConfig.Config nbtConfig = merged.get(
                    entity.getType(), NBT_CONDITION_ID, LivingEntity.class, entity);
            l2fix$addActivePresets(
                    nbtConfig, entity, difficulty, collector, nbtPresets);
        } catch (Exception exception) {
            l2fix$warnPresetFailure(null, exception);
        }

        return List.copyOf(nbtPresets);
    }

    private static void l2fix$addActivePresets(EntityConfig.Config config, LivingEntity entity,
                                                int difficulty, MobDifficultyCollector collector,
                                                List<EntityConfig.TraitBase> selected) {
        if (config == null || config.traits() == null) return;
        for (EntityConfig.TraitBase preset : config.traits()) {
            try {
                if (!(preset.condition() == null ||
                        preset.condition().match(entity, difficulty, collector))) continue;
                selected.add(preset);
            } catch (Exception exception) {
                l2fix$warnPresetFailure(preset, exception);
            }
        }
    }

    private static void l2fix$warnPresetFailure(EntityConfig.TraitBase preset, Exception exception) {
        long now = System.nanoTime();
        long next = NEXT_WARNING_NANOS.get();
        if (now < next || !NEXT_WARNING_NANOS.compareAndSet(next, now + WARNING_INTERVAL_NANOS)) return;
        String traitId = preset != null && preset.trait() != null
                ? preset.trait().getID() : "<config lookup>";
        LOGGER.warn("Skipped failed NBT trait preset {}. Further warnings are suppressed for 60 seconds.",
                traitId, exception);
    }
}
