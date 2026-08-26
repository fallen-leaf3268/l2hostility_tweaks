package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import com.l2hostility_tweaks.util.ImmunityHelper;
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

import java.util.Collection;

@Mixin(Item.class)
public class TraitSymbolSelfUseMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	public void l2fix$traitSymbolSelfUse(Level level, Player player, InteractionHand hand,
	                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof TraitSymbol traitSymbol)) return;
		if (!player.isShiftKeyDown()) return;
		if (!L2HConfig.isPlayerSelfTraitEnabled()) return;
		if (!MobTraitCap.HOLDER.isProper(player)) return;

		MobTraitCap cap = MobTraitCap.HOLDER.get(player);
		MobTrait trait = traitSymbol.get();

		if (ImmunityHelper.isSelfBlacklisted(trait)) {
			if (player instanceof ServerPlayer sp) {
				sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_BLACKLISTED, trait.getDesc()).withStyle(ChatFormatting.RED), true);
			}
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		var override = L2HConfig.getPlayerTraitOverrides().get(trait.getID());
		Integer rawLevel = cap.traits.get(trait);
		int currentLevel = rawLevel != null ? Math.abs(rawLevel) : 0;

		int maxTraits = L2HConfig.getPlayerMaxTraits();
		if (maxTraits >= 0) {
			int currentCount = l2fix$projectedTraitCount(cap.traits.values(), rawLevel);
			if (currentCount > maxTraits) {
				if (player instanceof ServerPlayer sp) {
					sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_MAX_COUNT, maxTraits).withStyle(ChatFormatting.RED), true);
				}
				cir.setReturnValue(InteractionResultHolder.fail(stack));
				return;
			}
		}

		if (l2fix$isAtMaxLevel(rawLevel, trait.getMaxLevel())) {
			if (player instanceof ServerPlayer sp) {
				sp.sendSystemMessage(LangData.MSG_ERR_MAX.get().withStyle(ChatFormatting.RED), true);
			}
			cir.setReturnValue(InteractionResultHolder.fail(stack));
			return;
		}

		if (L2HConfig.isExclusionEnabled()) {
			for (var group : L2HConfig.getExclusionGroups()) {
				if (group.traitIds().contains(trait.getID())) {
					for (var entry : cap.traits.entrySet()) {
						if (l2fix$isPresentForExclusion(entry.getValue()) && !entry.getKey().getID().equals(trait.getID()) && group.traitIds().contains(entry.getKey().getID())) {
							if (player instanceof ServerPlayer sp) {
								sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_MUTUAL_EXCLUSION, trait.getDesc(), entry.getKey().getDesc()).withStyle(ChatFormatting.RED), true);
							}
							cir.setReturnValue(InteractionResultHolder.fail(stack));
							return;
						}
					}
				}
			}
		}

		if (L2HConfig.isPlayerSelfTraitBalanceEnabled()) {
			int playerLevel = PlayerDifficulty.HOLDER.isProper(player)
					? PlayerDifficulty.HOLDER.get(player).getLevel().getLevel() : 0;

			int minLevel = override != null ? override.minLevel() : trait.getConfig().min_level;
			if (playerLevel < minLevel) {
				if (player instanceof ServerPlayer sp) {
					sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_MIN_LEVEL, minLevel, trait.getDesc(), playerLevel).withStyle(ChatFormatting.RED), true);
				}
				cir.setReturnValue(InteractionResultHolder.fail(stack));
				return;
			}

			int budget = (int) (playerLevel * L2HConfig.getPlayerSelfTraitBudgetRatio());
			int usedCost = 0;
			for (var entry : cap.traits.entrySet()) {
				var entryOverride = L2HConfig.getPlayerTraitOverrides().get(entry.getKey().getID());
				int entryCost = entryOverride != null ? entryOverride.cost() : entry.getKey().getConfig().cost;
				usedCost += entryCost * Math.abs(entry.getValue());
			}
			int nextCost = override != null ? override.cost() : trait.getConfig().cost;

			if (usedCost + nextCost > budget) {
				if (player instanceof ServerPlayer sp) {
					sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_BUDGET_EXCEEDED, trait.getDesc(), usedCost + nextCost, budget).withStyle(ChatFormatting.RED), true);
				}
				cir.setReturnValue(InteractionResultHolder.fail(stack));
				return;
			}
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

		if (level.isClientSide()) {
			cir.setReturnValue(InteractionResultHolder.success(stack));
			return;
		}

		float oldHealth = player.getHealth();
		float oldMax = player.getMaxHealth();
		TraitDisableHelper.clearSealData(player.getPersistentData(), trait.getID());
		player.getPersistentData().remove("l2htweaks_disabled_" + trait.getID());
		int val = cap.traits.compute(trait, (k, v) -> {
			int base = (v == null) ? 0 : Math.abs(v);
			return Math.min(base + 1, trait.getMaxLevel());
		});
		trait.initialize(player, val);
		trait.postInit(player, val);
		cap.syncToClient(player);
		float ratio = oldMax > 0 ? oldHealth / oldMax : 1.0f;
		player.setHealth(Math.max(1, player.getMaxHealth() * ratio));

		if (player instanceof ServerPlayer sp) {
			sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_ADDED, trait.getDesc(), val, cost).withStyle(ChatFormatting.GREEN), true);
			CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
			if (L2HConfig.isPlayerSelfTraitBalanceEnabled()) {
				int playerLevel = PlayerDifficulty.HOLDER.isProper(player)
						? PlayerDifficulty.HOLDER.get(player).getLevel().getLevel() : 0;
				int budget = (int) (playerLevel * L2HConfig.getPlayerSelfTraitBudgetRatio());
				int usedCost = 0;
				for (var entry : cap.traits.entrySet()) {
					var entryOverride = L2HConfig.getPlayerTraitOverrides().get(entry.getKey().getID());
					int entryCost = entryOverride != null ? entryOverride.cost() : entry.getKey().getConfig().cost;
					usedCost += entryCost * Math.abs(entry.getValue());
				}
				int minLevel = override != null ? override.minLevel() : trait.getConfig().min_level;
				sp.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SELF_TRAIT_COST_INFO, trait.getDesc(), usedCost, budget, minLevel).withStyle(ChatFormatting.GREEN), true);
			}
		}
		if (!player.getAbilities().instabuild) {
			stack.shrink(cost);
		}

		cir.setReturnValue(InteractionResultHolder.success(stack));
	}

	static boolean l2fix$isAtMaxLevel(Integer rawLevel, int maxLevel) {
		return rawLevel != null && Math.abs(rawLevel) >= maxLevel;
	}

	static int l2fix$projectedTraitCount(Collection<Integer> levels, Integer targetRawLevel) {
		int count = (int) levels.stream().filter(value -> value != null && value != 0).count();
		return targetRawLevel == null || targetRawLevel == 0 ? count + 1 : count;
	}

	static boolean l2fix$isPresentForExclusion(Integer rawLevel) {
		return rawLevel != null && rawLevel != 0;
	}
}
