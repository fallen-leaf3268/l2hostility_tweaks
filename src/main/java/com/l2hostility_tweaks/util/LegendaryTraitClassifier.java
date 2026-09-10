package com.l2hostility_tweaks.util;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class LegendaryTraitClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:legendary_traits");

    public static boolean isLegendary(MobTrait trait) {
        return classify(trait instanceof LegendaryTrait, traitId(trait), L2HConfig.getExtraLegendaryIds());
    }

    public static boolean isDisplayLegendary(MobTrait trait) {
        return classify(trait instanceof LegendaryTrait, traitId(trait), L2HConfig.getDisplayExtraLegendaryIds());
    }

    public static boolean isExtraLegendary(MobTrait trait) {
        return classifyExtra(trait instanceof LegendaryTrait, traitId(trait), L2HConfig.getExtraLegendaryIds());
    }

    public static boolean isDisplayExtraLegendary(MobTrait trait) {
        return classifyExtra(trait instanceof LegendaryTrait, traitId(trait), L2HConfig.getDisplayExtraLegendaryIds());
    }

    public static void validateConfiguredIds() {
        var registry = LHTraits.TRAITS.get();
        if (registry == null) return;
        Set<String> unknown = unknownIds(L2HConfig.getExtraLegendaryIds(), registry::containsKey);
        if (!unknown.isEmpty()) {
            LOGGER.warn("Unknown extra legendary trait IDs: {}", unknown);
        }
    }

    static boolean classify(boolean nativeLegendary, String traitId, Set<String> extraIds) {
        return nativeLegendary || traitId != null && extraIds.contains(traitId);
    }

    static boolean classifyExtra(boolean nativeLegendary, String traitId, Set<String> extraIds) {
        return !nativeLegendary && traitId != null && extraIds.contains(traitId);
    }

    static Set<String> unknownIds(Set<String> configuredIds, Predicate<ResourceLocation> exists) {
        Set<String> unknown = new LinkedHashSet<>();
        for (String configuredId : configuredIds) {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            if (id == null || !exists.test(id)) unknown.add(configuredId);
        }
        return unknown;
    }

    private static String traitId(MobTrait trait) {
        return trait == null ? null : trait.getID();
    }

    private LegendaryTraitClassifier() {
    }
}
