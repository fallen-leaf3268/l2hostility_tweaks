package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.traits.legendary.UndyingTrait;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UndyingTrait.class, remap = false)
public class UndyingTraitMixin {

	@Inject(method = "onDeath", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$limitResurrections(int level, LivingEntity entity, LivingDeathEvent event, CallbackInfo ci) {
		if (TraitDisableHelper.isDisabled(entity, TraitDisableHelper.UNDYING_TRAIT_ID)) {
			ci.cancel();
		}
	}

	@Inject(method = "onDeath", at = @At("TAIL"), remap = false)
	private void l2fix$incrementCount(int level, LivingEntity entity, LivingDeathEvent event, CallbackInfo ci) {
		if (!event.isCanceled()) return;

		int max = L2HConfig.getUndyingMaxResurrections();
		if (max < 0) return;

		CompoundTag pd = entity.getPersistentData();
		int count = pd.getInt(TraitDisableHelper.UNDYING_COUNT_KEY) + 1;
		pd.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, count);

		if (count >= max) {
			int duration = L2HConfig.getUndyingSealDuration();
			if (duration == 0) return;
			String sealKey = TraitDisableHelper.sealExpiryKey(TraitDisableHelper.UNDYING_TRAIT_ID);
			if (duration > 0) {
				long expiry = entity.level().getGameTime() + duration * 20L;
				pd.putLong(sealKey, expiry);
			} else {
				pd.putLong(sealKey, -1);
			}
			TraitDisableHelper.setDisabled(entity, TraitDisableHelper.UNDYING_TRAIT_ID, true);
		}
	}

}
