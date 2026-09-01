package com.l2hostility_tweaks.compat.kubejs;

import com.l2hostility_tweaks.content.traits.LegendaryTargetEffectTrait;
import dev.xkmc.l2hostility.compat.kubejs.AbstractTraitBuilder;
import dev.xkmc.l2hostility.content.capability.mob.PerformanceConstants;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.base.SelfEffectTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import dev.xkmc.l2library.base.effects.EffectUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public class SelfEffectTraitBuilder extends AbstractTraitBuilder<SelfEffectTraitBuilder> {

	private Supplier<MobEffect> effect = () -> MobEffects.WEAKNESS;
	private int duration = -1;
	private int amplifierPerLevel = 1;
	private String effectConfigurationError;
	private String durationConfigurationError;
	private String amplifierConfigurationError;

	public SelfEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public SelfEffectTraitBuilder effect(String id) {
		var eff = KubeJsRegistryResolver.resolve("mob effect", id,
				key -> ForgeRegistries.MOB_EFFECTS.getValue(key));
		if (eff != null) {
			this.effect = () -> eff;
			this.effectConfigurationError = null;
		} else {
			this.effectConfigurationError = "effect=" + id;
		}
		return this;
	}

	public SelfEffectTraitBuilder time(int ticks) {
		if (KubeJsRegistryResolver.validatePositive("self effect duration", ticks)) {
			this.duration = ticks;
			this.durationConfigurationError = null;
		} else {
			this.durationConfigurationError = "duration=" + ticks;
		}
		return this;
	}

	public SelfEffectTraitBuilder effectLevel(int amp) {
		if (KubeJsRegistryResolver.validateNonNegative("self effect amplifier per level", amp)) {
			this.amplifierPerLevel = amp;
			this.amplifierConfigurationError = null;
		} else {
			this.amplifierConfigurationError = "amplifierPerLevel=" + amp;
		}
		return this;
	}

	@Override
	public MobTrait createObject() {
		KubeJsRegistryResolver.requireValidTraitConfiguration(id, effectConfigurationError,
				durationConfigurationError, amplifierConfigurationError);
		var eff = effect;
		int dur = duration;
		int ampPerLvl = amplifierPerLevel;
		if (color != null || duration > 0 || amplifierPerLevel != 1) {
			return new SelfEffectTrait(eff) {
				@Override
				public void tick(LivingEntity mob, int level) {
					if (mob.level().isClientSide()) return;
					int d = dur > 0 ? dur : PerformanceConstants.selfEffectInterval();
					EffectUtil.refreshEffect(mob,
							new MobEffectInstance(eff.get(), d,
									LegendaryTargetEffectTrait.scaleAmplifier(level, ampPerLvl)),
							EffectUtil.AddReason.FORCE, mob);
				}

				@Override
				public void addDetail(List<Component> list) {
					list.add(LangData.TOOLTIP_SELF_EFFECT.get());
					ChatFormatting c = eff.get().getCategory().getTooltipFormatting();
					if (getMaxLevel() == 1) {
						list.add(eff.get().getDisplayName().copy().withStyle(c));
					} else list.add(mapLevel(e ->
							Component.translatable("potion.withAmplifier", eff.get().getDisplayName(),
									LegendaryTargetEffectTrait.formatAmplifier(
											LegendaryTargetEffectTrait.scaleAmplifier(e, ampPerLvl)))
									.withStyle(c))
					);
				}

				@Override
				public int getColor() {
					return color != null ? color.getAsInt() : super.getColor();
				}
			};
		}
		return new SelfEffectTrait(eff);
	}
}
