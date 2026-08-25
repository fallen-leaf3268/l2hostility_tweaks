package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.enchantments.RemoveTraitEnchantment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = RemoveTraitEnchantment.class, remap = false)
public class RemoveTraitEnchantmentMixin {

	@Inject(method = "hitMob", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$sealTraitInsteadOfRemove(LivingEntity target, MobTraitCap cap, Integer value, AttackCache cache, CallbackInfo ci) {
		ci.cancel();
		var trait = l2fix$findActiveSplit(cap.traits, mobTrait -> mobTrait.getID());
		if (trait == null) return;
		String traitId = trait.getID();

		TraitDisableHelper.setDisabled(target, traitId, true);
		target.getPersistentData().putLong(
				TraitDisableHelper.sealExpiryKey(traitId), l2fix$sealExpiry(value));
		cap.syncToClient(target);

		if (target instanceof Slime slime) {
			slime.addTag("SuppressSplit");
		}
	}

	@Unique
	static <T> T l2fix$findActiveSplit(Map<T, Integer> traits, Function<T, String> idGetter) {
		return traits.entrySet().stream()
				.filter(entry -> "l2hostility:split".equals(idGetter.apply(entry.getKey())))
				.filter(entry -> entry.getValue() != null && entry.getValue() > 0)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	@Unique
	static long l2fix$sealExpiry(Integer ignoredValue) {
		return -1L;
	}
}
