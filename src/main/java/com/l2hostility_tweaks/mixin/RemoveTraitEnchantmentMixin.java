package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.enchantments.RemoveTraitEnchantment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RemoveTraitEnchantment.class, remap = false)
public class RemoveTraitEnchantmentMixin {

	@Inject(method = "hitMob", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$sealTraitInsteadOfRemove(LivingEntity target, MobTraitCap cap, Integer value, AttackCache cache, CallbackInfo ci) {
		ci.cancel();
		var trait = TraitDisableHelper.findActiveSplitTrait(cap.traits, mobTrait -> mobTrait.getID());
		if (trait == null) return;
		String traitId = trait.getID();

		target.getPersistentData().putLong(
				TraitDisableHelper.sealExpiryKey(traitId), TraitDisableHelper.permanentSealExpiry(value));
		TraitDisableHelper.setDisabled(target, traitId, true);

		if (target instanceof Slime slime) {
			slime.addTag("SuppressSplit");
		}
	}
}
