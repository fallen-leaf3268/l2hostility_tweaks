package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TraitSymbol.class, remap = false)
public class TraitSymbolBudgetMixin {

	@Inject(method = "m_6880_", at = @At("HEAD"), cancellable = true)
	public void l2fix$checkTraitBudget(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (player.level().isClientSide()) return;
		if (!L2HConfig.isPlayerTraitLimitEnabled()) return;
		if (!MobTraitCap.HOLDER.isProper(target)) return;

		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		MobTrait trait = ((TraitSymbol) (Object) this).get();

		int mobLevel = cap.getLevel();
		int minLevel = trait.getConfig().min_level;
		if (mobLevel < minLevel) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.MOB_TRAIT_MIN_LEVEL, minLevel, trait.getDesc(), mobLevel).withStyle(ChatFormatting.RED), true);
			cir.setReturnValue(InteractionResult.FAIL);
			return;
		}

		int budget = (int) (mobLevel * L2HConfig.getPlayerTraitBudgetRatio());

		int usedCost = 0;
		for (var entry : cap.traits.entrySet()) {
			usedCost += entry.getKey().getConfig().cost * Math.abs(entry.getValue());
		}
		int nextCost = trait.getConfig().cost;

		if (usedCost + nextCost > budget) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.MOB_TRAIT_BUDGET_EXCEEDED, trait.getDesc(), usedCost + nextCost, budget).withStyle(ChatFormatting.RED), true);
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "m_6880_", at = @At("RETURN"))
	public void l2fix$showTraitBudgetAfterAdd(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (player.level().isClientSide()) return;
		if (!L2HConfig.isPlayerTraitLimitEnabled()) return;
		if (cir.getReturnValue() != InteractionResult.SUCCESS) return;
		if (!MobTraitCap.HOLDER.isProper(target)) return;

		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		MobTrait trait = ((TraitSymbol) (Object) this).get();
		int mobLevel = cap.getLevel();
		int budget = (int) (mobLevel * L2HConfig.getPlayerTraitBudgetRatio());

		int usedCost = 0;
		for (var entry : cap.traits.entrySet()) {
			usedCost += entry.getKey().getConfig().cost * Math.abs(entry.getValue());
		}

		player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.MOB_TRAIT_COST_INFO, trait.getDesc(), usedCost, budget).withStyle(ChatFormatting.GREEN), true);
	}
}
