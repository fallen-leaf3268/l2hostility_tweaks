package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.ImmunityHelper;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = TraitSymbol.class, remap = false)
public class TraitSymbolMixin {

	private static Registry<MobTrait> getTraitRegistry() {
		return TraitDisableHelper.getTraitRegistry();
	}

	@Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true, remap = true)
	private void l2fix$fixSealedLevel(ItemStack stack, Player player, LivingEntity target,
									  InteractionHand hand,
									  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<InteractionResult> cir) {
		if (!MobTraitCap.HOLDER.isProper(target)) return;
		if (player.level().isClientSide()) return;

		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		MobTrait trait = ((TraitSymbol) (Object) this).get();
		Integer raw = cap.traits.get(trait);
		if (raw == null || raw >= 0) return;

		int abs = Math.abs(raw);
			if (abs >= trait.getMaxLevel()) {
				if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
					sp.sendSystemMessage(LangData.MSG_ERR_MAX.get().withStyle(net.minecraft.ChatFormatting.RED), true);
				}
				cir.setReturnValue(InteractionResult.FAIL);
				return;
			}
		float oldHealth = target.getHealth();
		float oldMaxHealth = target.getMaxHealth();
		int next = -(abs + 1);
		target.getPersistentData().putInt("l2htweaks_sealed_level_" + trait.getID(), abs + 1);
		cap.traits.put(trait, next);
		trait.initialize(target, 0);
		cap.syncToClient(target);
		target.setHealth(l2fix$scaledHealth(oldHealth, oldMaxHealth, target.getMaxHealth()));
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	static float l2fix$scaledHealth(float oldHealth, float oldMaxHealth, float newMaxHealth) {
		float ratio = oldMaxHealth > 0 ? oldHealth / oldMaxHealth : 1.0f;
		return Math.max(1.0f, newMaxHealth * Math.min(1.0f, ratio));
	}

	@Inject(method = "m_7373_", at = @At("TAIL"))
	private void l2fix$addTooltips(ItemStack stack, Level level, List<Component> tooltip,
	                                TooltipFlag flag, CallbackInfo ci) {
		MobTrait trait = ((TraitSymbol) (Object) this).get();
		if (trait.isBanned()) return;

		if (!(trait instanceof LegendaryTrait)) {
			Set<String> extraIds = L2HConfig.getExtraLegendaryIds();
			if (!extraIds.isEmpty() && extraIds.contains(trait.getID())) {
				if (tooltip.size() < 2) {
					tooltip.add(LangData.TOOLTIP_LEGENDARY.get().withStyle(ChatFormatting.GOLD));
				} else {
					tooltip.add(2, LangData.TOOLTIP_LEGENDARY.get().withStyle(ChatFormatting.GOLD));
				}
				}
		}

		if (L2HConfig.isExclusionEnabled() && !ImmunityHelper.isSelfBlacklisted(trait)) {
			for (var group : L2HConfig.getExclusionGroups()) {
				if (group.traitIds().contains(trait.getID())) {
					for (var otherId : group.traitIds()) {
						if (!otherId.equals(trait.getID())) {
							Component otherName = l2fix$getTraitName(getTraitRegistry(), otherId);
							tooltip.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.mutual", otherName)
									.withStyle(ChatFormatting.WHITE));
						}
					}
				}
				}
		}

		if (ImmunityHelper.isSelfBlacklisted(trait)) {
			tooltip.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.disabled")
					.withStyle(ChatFormatting.RED));
		} else {
			var fixes = new ArrayList<Component>();

			if ("l2hostility:reprint".equals(trait.getID())) {
				fixes.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.reprint"));
				}
			if ("l2hostility:dispell".equals(trait.getID())) {
				fixes.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.dispell"));
				}
			if ("l2hostility:invisible".equals(trait.getID())) {
				fixes.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.invisible"));
				}
			if ("l2hostility:dementor".equals(trait.getID())) {
				fixes.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.dementor"));
				}

			var override = L2HConfig.getPlayerTraitOverrides().get(trait.getID());
			if (override != null) {
				fixes.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.override",
						override.minLevel(), override.cost()));
				}

			if (!fixes.isEmpty()) {
				tooltip.add(Component.translatable("tooltip.l2hostility_tweaks.player_override.prefix")
						.withStyle(ChatFormatting.GOLD));
				for (var fix : fixes) {
					tooltip.add(fix.copy().withStyle(ChatFormatting.DARK_AQUA));
				}
				}
		}
	}

	static Component l2fix$getTraitName(Registry<MobTrait> registry, String traitId) {
		ResourceLocation id = ResourceLocation.tryParse(traitId);
		if (registry != null && id != null) {
			MobTrait t = registry.get(id);
			if (t != null) return t.getDesc();
		}
		return Component.literal(traitId);
	}
}
