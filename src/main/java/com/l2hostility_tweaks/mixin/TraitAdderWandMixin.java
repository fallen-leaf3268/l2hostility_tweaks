package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.item.wand.TraitAdderWand;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TraitAdderWand.class, remap = false)
public class TraitAdderWandMixin {

	@Inject(method = "clickTarget", at = @At("HEAD"), cancellable = true)
	private void l2fix$handleSealedTrait(ItemStack stack, Player player, LivingEntity target, CallbackInfo ci) {
		if (!MobTraitCap.HOLDER.isProper(target)) return;
		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		MobTrait trait = TraitAdderWand.get(stack);
		Integer level = cap.traits.get(trait);
		if (level == null || level >= 0) return;

		int abs = Math.abs(level);
		target.getPersistentData().remove(TraitDisableHelper.sealExpiryKey(trait.getID()));

		if (!player.isShiftKeyDown() && abs >= trait.getMaxLevel()) {
			ci.cancel();
			return;
		}

		cap.traits.put(trait, abs);
	}
}
