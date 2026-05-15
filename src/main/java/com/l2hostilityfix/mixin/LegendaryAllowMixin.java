package com.l2hostilityfix.mixin;

import dev.xkmc.l2hostility.content.logic.TraitManager;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LegendaryTrait.class, remap = false)
public class LegendaryAllowMixin {

    @Redirect(method = "allow",
            at = @At(value = "INVOKE",
                    target = "Ldev/xkmc/l2hostility/content/logic/TraitManager;getMaxLevel()I"))
    private int l2fix$bypassLegendaryMaxLevel() {
        return -1;
    }
}
