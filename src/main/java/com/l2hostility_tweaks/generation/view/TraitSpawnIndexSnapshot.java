package com.l2hostility_tweaks.generation.view;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record TraitSpawnIndexSnapshot(long revision, List<MobTraitOverview> mobs, int warningCount) {
    public TraitSpawnIndexSnapshot {
        mobs = List.copyOf(mobs);
    }

    public record MobTraitOverview(
            ResourceLocation entityId,
            List<EntityConfigView> configs,
            List<PresetTraitView> presets,
            List<PoolTraitView> pool,
            List<BlockedTraitView> blocked,
            List<TraitDynamicConstraint> dynamicConstraints) {
        public MobTraitOverview {
            Objects.requireNonNull(entityId);
            configs = List.copyOf(configs);
            presets = List.copyOf(presets);
            pool = List.copyOf(pool);
            blocked = List.copyOf(blocked);
            dynamicConstraints = List.copyOf(dynamicConstraints);
        }

        public ResourceLocation pageId() {
            return entityId;
        }
    }

    public record EntityConfigView(
            ResourceLocation sourceId,
            String conditionJson,
            int minDifficulty,
            int baseDifficulty,
            double variation,
            double scale,
            double applyChance,
            double traitChance,
            double suppression,
            int minSpawnLevel,
            int maxLevel,
            int maxTraitCount,
            boolean presetTraitsOnly) {}

    public record PresetTraitView(
            ResourceLocation traitId,
            ResourceLocation itemId,
            int freeRank,
            int minRank,
            boolean cap,
            double chance,
            int conditionLevel,
            ResourceLocation advancementId,
            ResourceLocation sourceId,
            String conditionJson,
            List<TraitDynamicConstraint> dynamicConstraints) {
        public PresetTraitView {
            Objects.requireNonNull(traitId);
            Objects.requireNonNull(itemId);
            if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
                throw new IllegalArgumentException("chance must be finite and between 0 and 1");
            }
            dynamicConstraints = List.copyOf(dynamicConstraints);
        }
    }

    public record PoolTraitView(
            ResourceLocation traitId,
            ResourceLocation itemId,
            int weight,
            int minLevel,
            int cost,
            int maxRank) {
        public PoolTraitView {
            Objects.requireNonNull(traitId);
            Objects.requireNonNull(itemId);
        }
    }

    public record BlockedContextView(ResourceLocation sourceId, List<TraitBlockReason> reasons) {
        public BlockedContextView {
            reasons = List.copyOf(reasons);
        }
    }

    public record BlockedTraitView(
            ResourceLocation traitId,
            ResourceLocation itemId,
            List<BlockedContextView> contexts) {
        public BlockedTraitView {
            Objects.requireNonNull(traitId);
            Objects.requireNonNull(itemId);
            contexts = List.copyOf(contexts);
        }
    }
}
