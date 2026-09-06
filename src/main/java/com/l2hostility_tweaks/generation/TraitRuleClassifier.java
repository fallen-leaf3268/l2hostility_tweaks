package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;

import java.util.LinkedHashSet;
import java.util.List;

public final class TraitRuleClassifier {
    public record Context(
            boolean entityConfigBlacklist,
            boolean traitEntityBlacklist,
            boolean traitWhitelistMiss,
            boolean traitGloballyDisabled,
            boolean entityNoTrait,
            boolean preset,
            boolean presetTraitsOnly,
            boolean tweaksDisableRandom,
            boolean tweaksDisableAll,
            boolean tweaksDisableMobLevel) {}

    public static List<TraitBlockReason> classify(Context context) {
        LinkedHashSet<TraitBlockReason> reasons = new LinkedHashSet<>();
        if (context.entityConfigBlacklist()) reasons.add(TraitBlockReason.ENTITY_CONFIG_BLACKLIST);
        if (context.traitEntityBlacklist()) reasons.add(TraitBlockReason.TRAIT_ENTITY_BLACKLIST);
        if (context.traitWhitelistMiss()) reasons.add(TraitBlockReason.TRAIT_WHITELIST_MISS);
        if (context.traitGloballyDisabled()) reasons.add(TraitBlockReason.TRAIT_GLOBALLY_DISABLED);
        if (context.entityNoTrait()) reasons.add(TraitBlockReason.ENTITY_NO_TRAIT);
        if (!context.preset() && context.presetTraitsOnly()) reasons.add(TraitBlockReason.PRESET_ONLY);
        if (!context.preset() && context.tweaksDisableRandom()) reasons.add(TraitBlockReason.TWEAKS_DISABLE_RANDOM);
        if (context.tweaksDisableAll()) reasons.add(TraitBlockReason.TWEAKS_DISABLE_ALL);
        if (context.tweaksDisableMobLevel()) reasons.add(TraitBlockReason.TWEAKS_DISABLE_MOB_LEVEL);
        return List.copyOf(reasons);
    }

    private TraitRuleClassifier() {}
}
