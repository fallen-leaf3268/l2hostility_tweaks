package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.config.L2HConfig.ExclusionGroup;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.world.entity.LivingEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class TraitGenerationHelper {

    private static final Logger LOG = LoggerFactory.getLogger("L2HostilityFix/TraitGenHelper");

    private static Field ENTITY_FIELD;
    private static Field TRAITS_FIELD;
    private static Field MOB_LEVEL_FIELD;
    private static Field LEVEL_FIELD;
    private static Field INS_FIELD;
    private static volatile boolean fieldsDiscovered;

    public static LivingEntity getEntity(TraitGenerator self) {
        try {
            discoverFields();
            return ENTITY_FIELD != null ? (LivingEntity) ENTITY_FIELD.get(self) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static HashMap<MobTrait, Integer> getTraits(TraitGenerator self) {
        try {
            discoverFields();
            return TRAITS_FIELD != null ? (HashMap<MobTrait, Integer>) TRAITS_FIELD.get(self) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static int getMobLevel(TraitGenerator self) {
        try {
            discoverFields();
            return MOB_LEVEL_FIELD != null ? MOB_LEVEL_FIELD.getInt(self) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getLevel(TraitGenerator self) {
        try {
            discoverFields();
            return LEVEL_FIELD != null ? LEVEL_FIELD.getInt(self) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setLevel(TraitGenerator self, int value) {
        try {
            discoverFields();
            if (LEVEL_FIELD != null) LEVEL_FIELD.setInt(self, value);
        } catch (Exception ignored) {}
    }

    public static MobDifficultyCollector getIns(TraitGenerator self) {
        try {
            discoverFields();
            return INS_FIELD != null ? (MobDifficultyCollector) INS_FIELD.get(self) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void discoverFields() {
        if (fieldsDiscovered) return;
        fieldsDiscovered = true;
        for (Field f : TraitGenerator.class.getDeclaredFields()) {
            f.setAccessible(true);
            String name = f.getName();
            if (ENTITY_FIELD == null && name.equals("entity") && f.getType() == LivingEntity.class) {
                ENTITY_FIELD = f;
            } else if (TRAITS_FIELD == null && name.equals("traits") && HashMap.class.isAssignableFrom(f.getType())) {
                TRAITS_FIELD = f;
            } else if (MOB_LEVEL_FIELD == null && name.equals("mobLevel") && f.getType() == int.class) {
                MOB_LEVEL_FIELD = f;
            } else if (LEVEL_FIELD == null && name.equals("level") && f.getType() == int.class) {
                LEVEL_FIELD = f;
            } else if (INS_FIELD == null && name.equals("ins") && f.getType() == MobDifficultyCollector.class) {
                INS_FIELD = f;
            }
        }
        if (ENTITY_FIELD == null) LOG.error("[TraitGenerationHelper] Failed to find field 'entity' in TraitGenerator! Trait caps/limits may not work.");
        if (TRAITS_FIELD == null) LOG.error("[TraitGenerationHelper] Failed to find field 'traits' in TraitGenerator! Trait caps/limits may not work.");
        if (MOB_LEVEL_FIELD == null) LOG.error("[TraitGenerationHelper] Failed to find field 'mobLevel' in TraitGenerator! Trait caps/limits may not work.");
        if (LEVEL_FIELD == null) LOG.error("[TraitGenerationHelper] Failed to find field 'level' in TraitGenerator! Trait caps/limits may not work.");
        if (INS_FIELD == null) LOG.error("[TraitGenerationHelper] Failed to find field 'ins' in TraitGenerator! Trait caps/limits may not work.");
    }

    public static void applyExclusions(HashMap<MobTrait, Integer> traits, int diff) {
        List<ExclusionGroup> groups = L2HConfig.getExclusionGroups();
        Random rand = new Random();

        for (ExclusionGroup group : groups) {
            List<String> present = group.traitIds().stream()
                    .filter(id -> hasTraitById(traits, id))
                    .collect(Collectors.toList());

            if (present.size() <= 1) continue;

            String keep;
            if ("roll".equals(group.rule())) {
                keep = present.get(rand.nextInt(present.size()));
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

    public static void applyFinalFilters(TraitGenerator self) {
        LivingEntity entity = getEntity(self);
        HashMap<MobTrait, Integer> traits = getTraits(self);
        if (entity == null || !entity.isAlive() || traits == null || traits.isEmpty()) return;

        int difficulty = getMobLevel(self);
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
            applyExclusions(traits, difficulty);
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
