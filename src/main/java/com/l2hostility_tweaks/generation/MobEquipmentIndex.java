package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobEquipmentCategory;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobEquipmentRuleView;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobEquipmentView;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.init.L2Hostility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MobEquipmentIndex {

    private static final Set<String> WARNED_UNKNOWN_SLOTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED_INVALID_WEIGHTS = ConcurrentHashMap.newKeySet();

    public static List<MobEquipmentView> capture(EntityConfig.Config config, ResourceLocation sourceId) {
        List<MobEquipmentView> result = new ArrayList<>();
        List<String> slots = config.items.stream().map(EntityConfig.ItemPool::slot).toList();
        for (int poolIndex = 0; poolIndex < config.items.size(); poolIndex++) {
            EntityConfig.ItemPool pool = config.items.get(poolIndex);
            MobEquipmentCategory category = category(pool.slot());
            if (category == null) {
                warnUnknownSlotOnce(pool.slot());
                continue;
            }
            if (pool.entries().stream().anyMatch(entry -> entry.weight() < 0)) {
                warnInvalidWeightsOnce(sourceId, poolIndex);
                continue;
            }
            int totalWeight = pool.entries().stream().mapToInt(EntityConfig.ItemEntry::weight).sum();
            if (totalWeight <= 0) continue;
            boolean mayBeOverwritten = hasLaterSlot(slots, poolIndex);
            double chance = Math.max(0, Math.min(1, pool.chance()));
            for (EntityConfig.ItemEntry entry : pool.entries()) {
                if (entry.weight() <= 0 || entry.stack().isEmpty()) continue;
                MobEquipmentRuleView rule = new MobEquipmentRuleView(
                        sourceId, pool.slot(), Math.max(0, pool.level()), chance,
                        entry.weight(), totalWeight, mayBeOverwritten);
                merge(result, category, entry.stack(), rule);
            }
        }
        return List.copyOf(result);
    }

    static MobEquipmentCategory category(String slot) {
        if (slot == null) return null;
        return switch (slot) {
            case "equipment/head", "equipment/chest", "equipment/legs", "equipment/feet" ->
                    MobEquipmentCategory.ARMOR;
            case "equipment/mainhand", "equipment/offhand" -> MobEquipmentCategory.HANDS;
            default -> slot.startsWith("curios/") && slot.length() > "curios/".length()
                    ? MobEquipmentCategory.CURIOS : null;
        };
    }

    static boolean hasLaterSlot(List<String> slots, int poolIndex) {
        String slot = slots.get(poolIndex);
        for (int index = poolIndex + 1; index < slots.size(); index++) {
            if (slot.equals(slots.get(index))) return true;
        }
        return false;
    }

    private static void merge(List<MobEquipmentView> result, MobEquipmentCategory category,
                              ItemStack stack, MobEquipmentRuleView rule) {
        for (int index = 0; index < result.size(); index++) {
            MobEquipmentView existing = result.get(index);
            if (existing.category() != category || !ItemStack.isSameItemSameTags(existing.stack(), stack)) continue;
            List<MobEquipmentRuleView> rules = new ArrayList<>(existing.rules());
            rules.add(rule);
            result.set(index, new MobEquipmentView(category, stack, rules));
            return;
        }
        result.add(new MobEquipmentView(category, stack, List.of(rule)));
    }

    private static void warnUnknownSlotOnce(String slot) {
        String key = String.valueOf(slot);
        if (WARNED_UNKNOWN_SLOTS.add(key)) {
            L2Hostility.LOGGER.warn("Skipping JEI equipment display for unknown entity item slot '{}'", slot);
        }
    }

    private static void warnInvalidWeightsOnce(ResourceLocation sourceId, int poolIndex) {
        String key = sourceId + "#" + poolIndex;
        if (WARNED_INVALID_WEIGHTS.add(key)) {
            L2Hostility.LOGGER.warn("Skipping JEI equipment display for item pool {} in '{}' because it has a negative weight",
                    poolIndex, sourceId);
        }
    }

    private MobEquipmentIndex() {
    }
}
