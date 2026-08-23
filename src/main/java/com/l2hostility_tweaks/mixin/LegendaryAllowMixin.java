package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.logic.TraitManager;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LegendaryTrait.class, remap = false)
public class LegendaryAllowMixin {

    @Redirect(method = "allow",
            at = @At(value = "INVOKE",
                    target = "Ldev/xkmc/l2hostility/content/logic/TraitManager;getMaxLevel()I"))
    private int l2fix$bypassLegendaryMaxLevel() {
        return l2fix$resolveLegendaryGate(
                L2HConfig.COMMON.legendaryBypassVanillaGate.get(),
                TraitManager.getMaxLevel());
    }

    @Unique
    static int l2fix$resolveLegendaryGate(boolean bypass, int upstreamGate) {
        return bypass ? -1 : upstreamGate;
    }
}
