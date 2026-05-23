package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2hostility.content.traits.common.GravityTrait;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GravityTrait.class, remap = false)
public class GravityTraitMixin {

    @Redirect(method = "onDamaged",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private void preventGravityPushIfImmune(LivingEntity entity, double x, double y, double z) {
        if (ImmunityHelper.isImmuneToGravity(entity)) {
            return;
        }
        entity.push(x, y, z);
    }

    @Inject(method = "canApply", at = @At("HEAD"), cancellable = true, remap = false)
    private void preventGravityAuraIfImmune(LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
        if (ImmunityHelper.isImmuneToGravity(e)) {
            cir.setReturnValue(false);
        }
    }
}
