package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.ImmunityHelper;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
							Component otherName = getTraitName(otherId);
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

	private static Component getTraitName(String traitId) {
		Registry<MobTrait> reg = getTraitRegistry();
		if (reg != null) {
			MobTrait t = reg.get(new ResourceLocation(traitId));
			if (t != null) return t.getDesc();
		}
		return Component.literal(traitId);
	}
}
