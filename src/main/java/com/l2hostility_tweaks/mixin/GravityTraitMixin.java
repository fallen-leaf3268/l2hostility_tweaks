package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2hostility.content.traits.common.GravityTrait;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GravityTrait.class, remap = false)
public class GravityTraitMixin {

	@Inject(method = "canApply", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$preventGravityAuraIfImmune(LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
		if (ImmunityHelper.isImmuneToGravity(e)) {
			cir.setReturnValue(false);
		}
	}
}
