package com.l2hostility_tweaks.generation.view;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public record TraitSpawnIndexSnapshot(long revision, List<MobTraitOverview> mobs, int warningCount) {
    public TraitSpawnIndexSnapshot {
        mobs = List.copyOf(mobs);
    }

    public record MobTraitOverview(
            ResourceLocation entityId,
            int variantIndex,
            String jeiDisplayName,
            List<EntityConfigView> configs,
            List<PresetTraitView> presets,
            List<PoolTraitView> pool,
            List<BlockedTraitView> blocked,
            List<MobEquipmentView> equipment,
            List<TraitDynamicConstraint> dynamicConstraints) {
        public MobTraitOverview {
            Objects.requireNonNull(entityId);
            if (variantIndex < 0) throw new IllegalArgumentException("variantIndex must not be negative");
            jeiDisplayName = jeiDisplayName == null ? "" : jeiDisplayName;
            configs = List.copyOf(configs);
            presets = List.copyOf(presets);
            pool = List.copyOf(pool);
            blocked = List.copyOf(blocked);
            equipment = List.copyOf(equipment);
            dynamicConstraints = List.copyOf(dynamicConstraints);
        }

        public MobTraitOverview(ResourceLocation entityId, int variantIndex, String jeiDisplayName,
                                List<EntityConfigView> configs, List<PresetTraitView> presets,
                                List<PoolTraitView> pool, List<BlockedTraitView> blocked,
                                List<TraitDynamicConstraint> dynamicConstraints) {
            this(entityId, variantIndex, jeiDisplayName, configs, presets, pool, blocked,
                    List.of(), dynamicConstraints);
        }

        public MobTraitOverview(ResourceLocation entityId, List<EntityConfigView> configs,
                                List<PresetTraitView> presets, List<PoolTraitView> pool,
                                List<BlockedTraitView> blocked,
                                List<TraitDynamicConstraint> dynamicConstraints) {
            this(entityId, 0, "", configs, presets, pool, blocked, dynamicConstraints);
        }

        public MobTraitOverview(ResourceLocation entityId, int variantIndex,
                                List<EntityConfigView> configs, List<PresetTraitView> presets,
                                List<PoolTraitView> pool, List<BlockedTraitView> blocked,
                                List<TraitDynamicConstraint> dynamicConstraints) {
            this(entityId, variantIndex, "", configs, presets, pool, blocked, dynamicConstraints);
        }

        public ResourceLocation pageId() {
            if (variantIndex == 0) return entityId;
            return new ResourceLocation(entityId.getNamespace(),
                    entityId.getPath() + "/condition/" + variantIndex);
        }
    }

    public enum MobEquipmentCategory {
        ARMOR,
        HANDS,
        CURIOS
    }

    public record MobEquipmentRuleView(
            ResourceLocation sourceId,
            String slot,
            int minLevel,
            double poolChance,
            int weight,
            int totalWeight,
            boolean mayBeOverwritten) {
        public MobEquipmentRuleView {
            Objects.requireNonNull(slot);
            if (slot.isBlank()) throw new IllegalArgumentException("slot must not be blank");
            if (minLevel < 0) throw new IllegalArgumentException("minLevel must not be negative");
            if (!Double.isFinite(poolChance) || poolChance < 0 || poolChance > 1) {
                throw new IllegalArgumentException("poolChance must be finite and between 0 and 1");
            }
            if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
            if (totalWeight < weight) throw new IllegalArgumentException("totalWeight must include weight");
        }

        public double poolSelectionChance() {
            return (double) weight / totalWeight;
        }

        public double ruleHitChance() {
            return poolChance * poolSelectionChance();
        }
    }

    public record MobEquipmentView(
            MobEquipmentCategory category,
            CompoundTag stackTag,
            List<MobEquipmentRuleView> rules) {
        public MobEquipmentView {
            Objects.requireNonNull(category);
            Objects.requireNonNull(stackTag);
            if (stackTag.isEmpty()) throw new IllegalArgumentException("stackTag must not be empty");
            stackTag = stackTag.copy();
            rules = List.copyOf(rules);
            if (rules.isEmpty()) throw new IllegalArgumentException("rules must not be empty");
        }

        @Override
        public CompoundTag stackTag() {
            return stackTag.copy();
        }

        public MobEquipmentView(MobEquipmentCategory category, ItemStack stack,
                                List<MobEquipmentRuleView> rules) {
            this(category, save(stack), rules);
        }

        public ItemStack stack() {
            return ItemStack.of(stackTag);
        }

        private static CompoundTag save(ItemStack stack) {
            Objects.requireNonNull(stack);
            if (stack.isEmpty()) throw new IllegalArgumentException("stack must not be empty");
            return stack.save(new CompoundTag());
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
            int guaranteedRank,
            List<TraitDynamicConstraint> dynamicConstraints) {
        public PresetTraitView {
            Objects.requireNonNull(traitId);
            Objects.requireNonNull(itemId);
            if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
                throw new IllegalArgumentException("chance must be finite and between 0 and 1");
            }
            if (guaranteedRank < 0) {
                throw new IllegalArgumentException("guaranteedRank must not be negative");
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
