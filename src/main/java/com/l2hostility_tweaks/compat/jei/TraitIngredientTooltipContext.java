package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record TraitIngredientTooltipContext(
        Section section,
        TraitSpawnIndexSnapshot.MobTraitOverview overview,
        ResourceLocation itemId) {

    public TraitIngredientTooltipContext {
        Objects.requireNonNull(section);
        Objects.requireNonNull(overview);
        Objects.requireNonNull(itemId);
    }

    public List<Component> tooltipLines() {
        return switch (section) {
            case POOL -> poolLines();
            case BLOCKED -> blockedLines();
            case PRESET -> presetLines();
        };
    }

    private List<Component> poolLines() {
        return TraitOverviewPresentation.poolForItem(overview, itemId).stream()
                .map(entry -> Component.translatable(
                        "jei.l2hostility_tweaks.max_rank", entry.maxRank())
                        .withStyle(ChatFormatting.GRAY))
                .map(Component.class::cast)
                .toList();
    }

    private List<Component> blockedLines() {
        LinkedHashSet<String> reasonKeys = new LinkedHashSet<>();
        TraitOverviewPresentation.blockedForItem(overview, itemId).forEach(entry ->
                reasonKeys.addAll(TraitOverviewPresentation.blockedReasonKeys(entry)));
        if (reasonKeys.isEmpty()) return List.of();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.l2hostility_tweaks.blocked_reason_header")
                .withStyle(ChatFormatting.RED));
        reasonKeys.stream()
                .map(key -> Component.translatable(key).withStyle(ChatFormatting.GRAY))
                .forEach(lines::add);
        return List.copyOf(lines);
    }

    private List<Component> presetLines() {
        List<TraitSpawnIndexSnapshot.PresetTraitView> presets =
                TraitOverviewPresentation.presetsForItem(overview, itemId);
        if (presets.isEmpty()) return List.of();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.l2hostility_tweaks.preset_details_header")
                .withStyle(ChatFormatting.GOLD));
        boolean numbered = presets.size() > 1;
        for (int index = 0; index < presets.size(); index++) {
            TraitSpawnIndexSnapshot.PresetTraitView preset = presets.get(index);
            if (numbered) {
                lines.add(Component.translatable("jei.l2hostility_tweaks.preset_entry", index + 1)
                        .withStyle(ChatFormatting.YELLOW));
            }
            lines.add(detail("jei.l2hostility_tweaks.source",
                    TraitOverviewPresentation.formatEntityConfigPath(preset.sourceId())));
            lines.add(detail("jei.l2hostility_tweaks.preset_chance",
                    TraitOverviewPresentation.formatPercent(preset.chance())));
            lines.add(detail("jei.l2hostility_tweaks.free_rank", preset.freeRank()));
            lines.add(detail("jei.l2hostility_tweaks.min_rank", preset.minRank()));
            lines.add(detail("jei.l2hostility_tweaks.preset_stop_random", preset.cap()));
            if (preset.conditionLevel() > 0) {
                lines.add(detail("jei.l2hostility_tweaks.condition_level", preset.conditionLevel()));
            }
            if (preset.advancementId() != null) {
                lines.add(detail("jei.l2hostility_tweaks.advancement", preset.advancementId()));
            }
            if (preset.conditionJson() != null && !preset.conditionJson().isBlank()) {
                lines.add(detail("jei.l2hostility_tweaks.condition",
                        TraitOverviewPresentation.compactConditionJson(preset.conditionJson())));
            }
        }
        return List.copyOf(lines);
    }

    private static Component detail(String key, Object value) {
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    public enum Section {
        POOL,
        BLOCKED,
        PRESET
    }
}
