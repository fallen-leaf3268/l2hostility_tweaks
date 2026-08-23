package com.l2hostility_tweaks.compat.kubejs;

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

	public SelfEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public SelfEffectTraitBuilder effect(String id) {
		var eff = KubeJsRegistryResolver.resolve("mob effect", id, ForgeRegistries.MOB_EFFECTS::getValue);
		if (eff != null) this.effect = () -> eff;
		return this;
	}

	public SelfEffectTraitBuilder time(int ticks) {
		this.duration = ticks;
		return this;
	}

	public SelfEffectTraitBuilder effectLevel(int amp) {
		this.amplifierPerLevel = amp;
		return this;
	}

	@Override
	public MobTrait createObject() {
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
							new MobEffectInstance(eff.get(), d, (level - 1) * ampPerLvl),
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
									Component.translatable("potion.potency." + (e - 1) * ampPerLvl))
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
