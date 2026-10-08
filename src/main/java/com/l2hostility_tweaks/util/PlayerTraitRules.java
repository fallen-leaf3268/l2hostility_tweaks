package com.l2hostility_tweaks.util;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import com.l2hostility_tweaks.mixin.MobTraitPendingAccessor;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerTraitRules {

    public enum Reason {
        NONE, DISALLOWED, SEALED, EXCLUSION, MAX_TRAITS, MIN_LEVEL, BUDGET
    }

    public record Limits(int maxTraits, int playerLevel, boolean balanceEnabled,
                         double budgetRatio, List<L2HConfig.ExclusionGroup> exclusions) {
        public long budget() {
            return (long) (Math.max(0, playerLevel) * budgetRatio);
        }
    }

    public record Result(Reason reason, long usedCost, long budget, int minLevel, String conflict) {
        public boolean allowed() {
            return reason == Reason.NONE;
        }
    }

    public static Result checkAddition(Player player, MobTraitCap cap, MobTrait candidate,
                                       int newLevel, boolean preserveSeal) {
        if (candidate.isBanned() || ImmunityHelper.isSelfBlacklisted(candidate)) {
            return new Result(Reason.DISALLOWED, 0, 0, 0, null);
        }
        if (preserveSeal && cap.traits.getOrDefault(candidate, 0) < 0) {
            return new Result(Reason.SEALED, 0, 0, 0, null);
        }
        Map<MobTrait, Integer> effective = effectiveTraits(cap);
        Map<String, Integer> levels = new LinkedHashMap<>();
        Map<String, Integer> costs = new LinkedHashMap<>();
        var overrides = L2HConfig.getPlayerTraitOverrides();
        for (var entry : effective.entrySet()) {
            MobTrait trait = entry.getKey();
            levels.put(trait.getID(), entry.getValue());
            var override = overrides.get(trait.getID());
            costs.put(trait.getID(), override == null ? trait.getConfig().cost : override.cost());
        }
        var override = overrides.get(candidate.getID());
        int minLevel = override == null ? candidate.getConfig().min_level : override.minLevel();
        costs.put(candidate.getID(), override == null ? candidate.getConfig().cost : override.cost());
        int playerLevel = PlayerDifficulty.HOLDER.isProper(player)
                ? PlayerDifficulty.HOLDER.get(player).getLevel().getLevel() : 0;
        Limits limits = new Limits(L2HConfig.getPlayerMaxTraits(), playerLevel,
                L2HConfig.isPlayerSelfTraitBalanceEnabled(), L2HConfig.getPlayerSelfTraitBudgetRatio(),
                L2HConfig.isExclusionEnabled() ? L2HConfig.getExclusionGroups() : List.of());
        return evaluateAddition(levels, costs, candidate.getID(), newLevel, minLevel, limits, preserveSeal);
    }

    public static Integer getEffectiveTraitLevel(MobTraitCap cap, MobTrait trait) {
        return effectiveTraits(cap).get(trait);
    }

    public static void discardPendingTrait(MobTraitCap cap, MobTrait trait) {
        ((MobTraitPendingAccessor) cap).l2fix$getPendingTraits()
                .removeIf(entry -> entry.getFirst() == trait);
    }

    private static Map<MobTrait, Integer> effectiveTraits(MobTraitCap cap) {
        return withPending(cap.traits, ((MobTraitPendingAccessor) cap).l2fix$getPendingTraits());
    }

    public static <T> Map<T, Integer> withPending(Map<T, Integer> levels,
                                                 Collection<Pair<T, Integer>> pending) {
        Map<T, Integer> effective = new LinkedHashMap<>(levels);
        for (Pair<T, Integer> entry : pending) {
            effective.put(entry.getFirst(), entry.getSecond());
        }
        for (Pair<T, Integer> entry : pending) {
            if (entry.getSecond() == 0) effective.remove(entry.getFirst());
        }
        return effective;
    }

    public static Result evaluateAddition(Map<String, Integer> levels, Map<String, Integer> costs,
                                          String candidate, int newLevel, int minLevel,
                                          Limits limits, boolean preserveSeal) {
        Integer current = levels.get(candidate);
        long budget = limits.budget();
        if (preserveSeal && current != null && current < 0) {
            return new Result(Reason.SEALED, 0, budget, minLevel, null);
        }
        String conflict = TraitGenerationHelper.findExclusionConflict(candidate, levels, limits.exclusions());
        if (conflict != null) {
            return new Result(Reason.EXCLUSION, 0, budget, minLevel, conflict);
        }
        int count = 0;
        long usedCost = 0;
        for (var entry : levels.entrySet()) {
            if (entry.getKey().equals(candidate)) continue;
            Integer level = entry.getValue();
            if (level == null || level == 0) continue;
            count++;
            long cost = Math.max(0, costs.getOrDefault(entry.getKey(), 0)) * Math.abs((long) level);
            usedCost = saturatedAdd(usedCost, cost);
        }
        if (newLevel > 0) {
            count++;
            usedCost = saturatedAdd(usedCost, (long) Math.max(0, costs.getOrDefault(candidate, 0)) * newLevel);
        }
        Reason reason = limits.maxTraits() >= 0 && count > limits.maxTraits() ? Reason.MAX_TRAITS
                : limits.balanceEnabled() && limits.playerLevel() < minLevel ? Reason.MIN_LEVEL
                : limits.balanceEnabled() && usedCost > budget ? Reason.BUDGET : Reason.NONE;
        return new Result(reason, usedCost, budget, minLevel, null);
    }

    private static long saturatedAdd(long first, long second) {
        return first > Long.MAX_VALUE - second ? Long.MAX_VALUE : first + second;
    }

    private PlayerTraitRules() {
    }
}
