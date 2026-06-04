package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.loot.TraitLootCondition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 封印词条（value < 0）仍能正常参与掉落判定。
 * TraitLootCondition 原本用 hasTrait() 检查（要求 > 0），
 * 封印后 value = -3 会被误判为"没有此词条"。
 * 改为用绝对值比较等级范围。
 */
@Mixin(value = TraitLootCondition.class, remap = false)
public class TraitLootConditionMixin {

	@Shadow
	private dev.xkmc.l2hostility.content.traits.base.MobTrait trait;

	@Shadow
	private int minLevel;

	@Shadow
	private int maxLevel;

	@Inject(method = "test", at = @At("HEAD"), cancellable = true)
	private void l2fix$allowSealedDrop(LootContext ctx, CallbackInfoReturnable<Boolean> cir) {
		if (ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity le) {
			if (!MobTraitCap.HOLDER.isProper(le)) return;
			int lv = MobTraitCap.HOLDER.get(le).traits.getOrDefault(trait, 0);
			if (lv > 0) return; // 非封印，走原逻辑
			if (lv < 0) {
				cir.setReturnValue(-lv >= minLevel && -lv <= maxLevel);
			}
		}
	}
}
