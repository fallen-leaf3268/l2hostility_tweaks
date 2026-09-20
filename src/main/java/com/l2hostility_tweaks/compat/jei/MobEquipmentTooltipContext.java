package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record MobEquipmentTooltipContext(
        TraitSpawnIndexSnapshot.MobTraitOverview overview,
        TraitSpawnIndexSnapshot.MobEquipmentCategory category) {

    public MobEquipmentTooltipContext {
        Objects.requireNonNull(overview);
        Objects.requireNonNull(category);
    }

    public List<Component> tooltipLines(CompoundTag currentStack) {
        List<TraitSpawnIndexSnapshot.MobEquipmentRuleView> rules = overview.equipment().stream()
                .filter(entry -> entry.category() == category)
                .filter(entry -> sameStack(entry.stackTag(), currentStack))
                .flatMap(entry -> entry.rules().stream())
                .toList();
        if (rules.isEmpty()) return List.of();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.l2hostility_tweaks.equipment_details_header")
                .withStyle(ChatFormatting.GOLD));
        boolean numbered = rules.size() > 1;
        for (int index = 0; index < rules.size(); index++) {
            TraitSpawnIndexSnapshot.MobEquipmentRuleView rule = rules.get(index);
            if (numbered) {
                lines.add(Component.translatable("jei.l2hostility_tweaks.equipment_rule", index + 1)
                        .withStyle(ChatFormatting.YELLOW));
            }
            lines.add(detail("jei.l2hostility_tweaks.source",
                    TraitOverviewPresentation.formatEntityConfigPath(rule.sourceId())));
            lines.add(detail("jei.l2hostility_tweaks.equipment_slot", rule.slot()));
            lines.add(detail("jei.l2hostility_tweaks.equipment_min_level", rule.minLevel()));
            lines.add(detail("jei.l2hostility_tweaks.equipment_pool_chance",
                    TraitOverviewPresentation.formatPercent(rule.poolChance())));
            lines.add(detail("jei.l2hostility_tweaks.equipment_weight",
                    rule.weight(), rule.totalWeight()));
            lines.add(detail("jei.l2hostility_tweaks.equipment_selection_chance",
                    TraitOverviewPresentation.formatPercent(rule.poolSelectionChance())));
            lines.add(detail("jei.l2hostility_tweaks.equipment_rule_hit_chance",
                    TraitOverviewPresentation.formatPercent(rule.ruleHitChance())));
            if (rule.mayBeOverwritten()) {
                lines.add(Component.translatable("jei.l2hostility_tweaks.equipment_may_be_overwritten")
                        .withStyle(ChatFormatting.RED));
            }
        }
        return List.copyOf(lines);
    }

    static boolean sameStack(CompoundTag first, CompoundTag second) {
        if (first == null || second == null) return false;
        return first.getString("id").equals(second.getString("id"))
                && first.getCompound("tag").equals(second.getCompound("tag"));
    }

    private static Component detail(String key, Object... values) {
        return Component.translatable(key, values).withStyle(ChatFormatting.GRAY);
    }
}
