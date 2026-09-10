package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.LegendaryTraitClassifier;
import dev.xkmc.l2hostility.content.logic.TraitManager;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.data.LHConfig;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTrait.class, remap = false)
public abstract class MobTraitLegendaryMixin {

    @Inject(method = "allow(Lnet/minecraft/world/entity/LivingEntity;II)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void l2fix$applyLegendaryGate(LivingEntity entity, int difficulty, int maxLevel,
                                          CallbackInfoReturnable<Boolean> cir) {
        MobTrait trait = (MobTrait) (Object) this;
        boolean extraLegendary = LegendaryTraitClassifier.isExtraLegendary(trait);
        if (!l2fix$passesGate(extraLegendary,
                L2HConfig.COMMON.legendaryBypassVanillaGate.get(),
                maxLevel, TraitManager.getMaxLevel())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isBanned()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void l2fix$applyLegendaryToggle(CallbackInfoReturnable<Boolean> cir) {
        MobTrait trait = (MobTrait) (Object) this;
        if (l2fix$isBannedByLegendaryToggle(
                LegendaryTraitClassifier.isExtraLegendary(trait),
                LHConfig.COMMON.allowLegendary.get())) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean l2fix$passesGate(boolean extraLegendary, boolean bypass,
                                             int candidateMaxLevel, int upstreamMaxLevel) {
        return !extraLegendary || bypass || candidateMaxLevel > upstreamMaxLevel;
    }

    @Unique
    private static boolean l2fix$isBannedByLegendaryToggle(boolean extraLegendary,
                                                            boolean allowLegendary) {
        return extraLegendary && !allowLegendary;
    }
}
