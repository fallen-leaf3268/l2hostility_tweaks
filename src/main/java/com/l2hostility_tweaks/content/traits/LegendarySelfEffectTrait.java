package com.l2hostility_tweaks.content.traits;

import dev.xkmc.l2hostility.content.capability.mob.PerformanceConstants;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import dev.xkmc.l2library.base.effects.EffectUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class LegendarySelfEffectTrait extends LegendaryTrait {

	private final Supplier<MobEffect> effect;
	private final int duration;
	private final int amplifierPerLevel;
	private final IntSupplier color;

	public LegendarySelfEffectTrait(IntSupplier color, Supplier<MobEffect> effect, int duration, int amplifierPerLevel) {
		super(ChatFormatting.GOLD);
		this.color = color;
		this.effect = effect;
		this.duration = duration;
		this.amplifierPerLevel = amplifierPerLevel;
	}

	@Override
	public int getColor() {
		return color.getAsInt();
	}

	@Override
	public void tick(LivingEntity mob, int level) {
		if (mob.level().isClientSide()) return;
		int d = duration > 0 ? duration : PerformanceConstants.selfEffectInterval();
		EffectUtil.refreshEffect(mob,
				new MobEffectInstance(effect.get(), d, (level - 1) * amplifierPerLevel),
				EffectUtil.AddReason.FORCE, mob);
	}

	@Override
	public void addDetail(List<Component> list) {
		super.addDetail(list);
		list.add(LangData.TOOLTIP_SELF_EFFECT.get());
		ChatFormatting c = effect.get().getCategory().getTooltipFormatting();
		if (getMaxLevel() == 1) {
			list.add(effect.get().getDisplayName().copy().withStyle(c));
		} else {
			list.add(mapLevel(e ->
					Component.translatable("potion.withAmplifier", effect.get().getDisplayName(),
							Component.translatable("potion.potency." + (e - 1) * amplifierPerLevel))
							.withStyle(c)));
		}
	}
}
