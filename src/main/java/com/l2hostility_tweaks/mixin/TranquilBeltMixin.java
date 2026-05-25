package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.TranquilBeltItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class TranquilBeltMixin {

	@Inject(method = "handleDamageEvent", at = @At("HEAD"), cancellable = true)
	private void l2fix$cancelDamageEvent(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player && TranquilBeltItem.isWearing(self)) {
			ci.cancel();
		}
	}

	@Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
	private void l2fix$cancelKnockback(double strength, double x, double z, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player && TranquilBeltItem.isWearing(self)) {
			ci.cancel();
		}
	}
}
