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
		// 优先找 split 词条（防止史莱姆分裂），没有则封印第一个词条
		String traitId = cap.traits.keySet().stream()
				.filter(t -> t.getID().equals("l2hostility:split"))
				.findFirst().map(t -> t.getID()).orElse(null);
		if (traitId == null) {
			traitId = cap.traits.keySet().stream()
					.filter(t -> t.getID() != null && cap.getTraitLevel(t) > 0)
					.findFirst().map(t -> t.getID()).orElse(null);
		}
		if (traitId == null) return;

		ci.cancel();
		if (TraitDisableHelper.isDisabled(target, traitId)) return;

		TraitDisableHelper.setDisabled(target, traitId, true);
		// 使用附魔等级计算封印时长：等级 1 = 永久，2+ = value * 30 秒
		long duration = value != null && value > 1 ? value * 600L : -1L;
		target.getPersistentData().putLong(TraitDisableHelper.sealExpiryKey(traitId), duration);
		cap.syncToClient(target);

		if (target instanceof Slime slime) {
			slime.addTag("SuppressSplit");
		}
	}
}
