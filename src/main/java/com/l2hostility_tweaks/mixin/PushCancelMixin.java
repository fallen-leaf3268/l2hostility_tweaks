package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class PushCancelMixin {

	@Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
	private void l2fix$cancelPushIfImmune(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof LivingEntity le) {
			if (l2fix$shouldCancelPush(
					ImmunityHelper.isImmuneToForce(le),
					ImmunityHelper.isImmuneToGravity(le))) {
				ci.cancel();
			}
		}
	}

	@Unique
	static boolean l2fix$shouldCancelPush(boolean forceImmune, boolean gravityImmune) {
		return forceImmune;
	}
}
