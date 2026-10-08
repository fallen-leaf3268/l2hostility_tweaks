package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import com.l2hostility_tweaks.util.ImmunityHelper;
import com.l2hostility_tweaks.util.PlayerTraitRules;
import com.l2hostility_tweaks.util.TraitCostHelper;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class TraitSymbolSelfUseMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	public void l2fix$traitSymbolSelfUse(Level level, Player player, InteractionHand hand,
	                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof TraitSymbol traitSymbol)) return;
		if (!player.isShiftKeyDown()) return;
		if (level.isClientSide()) {
			if (!L2HConfig.isDisplayPlayerSelfTraitEnabled()) return;
			cir.setReturnValue(InteractionResultHolder.success(stack));
			return;
		}
		if (!L2HConfig.isPlayerSelfTraitEnabled()) return;
		if (!MobTraitCap.HOLDER.isProper(player)) return;

		MobTraitCap cap = MobTraitCap.HOLDER.get(player);
		MobTrait trait = traitSymbol.get();
		if (trait.isBanned()) {
			player.displayClientMessage(LangData.MSG_ERR_DISALLOW.get().withStyle(ChatFormatting.RED), true);
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		if (ImmunityHelper.isSelfBlacklisted(trait)) {
			if (player instanceof ServerPlayer sp) {
				sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_BLACKLISTED, trait.getDesc()).withStyle(ChatFormatting.RED), true);
			}
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		Integer rawLevel = PlayerTraitRules.getEffectiveTraitLevel(cap, trait);
		int currentLevel = rawLevel != null ? TraitCostHelper.normalizeStoredLevel(rawLevel) : 0;

		if (l2fix$isAtMaxLevel(rawLevel, trait.getMaxLevel())) {
			if (player instanceof ServerPlayer sp) {
				sp.sendSystemMessage(LangData.MSG_ERR_MAX.get().withStyle(ChatFormatting.RED), true);
			}
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		int val = currentLevel + 1;
		var addition = PlayerTraitRules.checkAddition(player, cap, trait, val, false);
		if (!addition.allowed()) {
			l2fix$reportRejection(player, cap, trait, addition);
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		int cost = L2HConfig.getUpgradeCost(currentLevel, stack.getMaxStackSize());
		if (cost == TraitCostHelper.UNPAYABLE) {
			if (player instanceof ServerPlayer sp) {
				sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.UPGRADE_UNPAYABLE)
						.withStyle(ChatFormatting.RED), true);
			}
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		if (!player.getAbilities().instabuild && stack.getCount() < cost) {
			if (player instanceof ServerPlayer sp) {
				sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_NOT_ENOUGH_ITEMS, trait.getDesc(), currentLevel, currentLevel + 1, cost, stack.getCount()).withStyle(ChatFormatting.RED), true);
			}
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		float oldHealth = player.getHealth();
		float oldMax = player.getMaxHealth();
		PlayerTraitRules.discardPendingTrait(cap, trait);
		TraitDisableHelper.clearSealData(player.getPersistentData(), trait.getID());
		player.getPersistentData().remove("l2htweaks_disabled_" + trait.getID());
		cap.traits.put(trait, val);
		trait.initialize(player, val);
		trait.postInit(player, val);
		cap.syncToClient(player);
		float ratio = oldMax > 0 ? oldHealth / oldMax : 1.0f;
		player.setHealth(Math.max(1, player.getMaxHealth() * ratio));

		if (player instanceof ServerPlayer sp) {
			sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_ADDED, trait.getDesc(), val, cost).withStyle(ChatFormatting.GREEN), true);
			CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
			if (L2HConfig.isPlayerSelfTraitBalanceEnabled()) {
				var total = PlayerTraitRules.checkAddition(player, cap, trait, val, false);
				sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_COST_INFO,
						trait.getDesc(), total.usedCost(), total.budget(), total.minLevel())
						.withStyle(ChatFormatting.GREEN), true);
			}
		}
		if (!player.getAbilities().instabuild) {
			stack.shrink(cost);
		}

		cir.setReturnValue(InteractionResultHolder.success(stack));
	}

	private static boolean l2fix$isAtMaxLevel(Integer rawLevel, int maxLevel) {
		return rawLevel != null && Math.abs((long) rawLevel) >= maxLevel;
	}

	private static void l2fix$reportRejection(Player player, MobTraitCap cap, MobTrait trait,
	                                         PlayerTraitRules.Result result) {
		var message = switch (result.reason()) {
			case EXCLUSION -> {
				MobTrait other = cap.traits.keySet().stream()
						.filter(candidate -> candidate.getID().equals(result.conflict())).findFirst().orElse(null);
				yield L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_MUTUAL_EXCLUSION,
						trait.getDesc(), other == null ? net.minecraft.network.chat.Component.literal(result.conflict()) : other.getDesc());
			}
			case MAX_TRAITS -> L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_MAX_COUNT, L2HConfig.getPlayerMaxTraits());
			case MIN_LEVEL -> L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_MIN_LEVEL,
					result.minLevel(), trait.getDesc(), PlayerDifficulty.HOLDER.isProper(player)
							? PlayerDifficulty.HOLDER.get(player).getLevel().getLevel() : 0);
			case BUDGET -> L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_BUDGET_EXCEEDED,
					trait.getDesc(), result.usedCost(), result.budget());
			default -> LangData.MSG_ERR_DISALLOW.get();
		};
		player.displayClientMessage(message.withStyle(ChatFormatting.RED), true);
	}

}
