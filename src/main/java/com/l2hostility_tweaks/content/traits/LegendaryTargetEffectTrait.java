package com.l2hostility_tweaks.content.traits;

import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import dev.xkmc.l2library.base.effects.EffectUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class LegendaryTargetEffectTrait extends LegendaryTrait {

	public final Function<Integer, MobEffectInstance> func;
	private final IntSupplier color;

	public LegendaryTargetEffectTrait(IntSupplier color, Function<Integer, MobEffectInstance> func) {
		super(ChatFormatting.GOLD);
		this.color = LegendaryTraitColor.normalize(color);
		this.func = func;
	}

	@Override
	public int getColor() {
		return color.getAsInt();
	}

	@Override
	public void postHurtImpl(int level, LivingEntity attacker, LivingEntity target) {
		EffectUtil.addEffect(target, func.apply(level), EffectUtil.AddReason.NONE, attacker);
	}

	public static Component formatAmplifier(int amplifier) {
		return amplifier <= 5 ? Component.translatable("potion.potency." + amplifier) :
				Component.literal(Long.toString((long) amplifier + 1));
	}

	public static int scaleAmplifier(int level, int amplifierPerLevel) {
		long scaled = ((long) level - 1) * amplifierPerLevel;
		return scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
	}

	@Override
	public void addDetail(List<Component> list) {
		super.addDetail(list);
		list.add(LangData.TOOLTIP_TARGET_EFFECT.get());
		list.add(mapLevel(e -> {
			MobEffectInstance ins = func.apply(e);
			MutableComponent ans = Component.translatable(ins.getDescriptionId());
			MobEffect mobeffect = ins.getEffect();
			if (ins.getAmplifier() > 0) {
				ans = Component.translatable("potion.withAmplifier", ans,
						formatAmplifier(ins.getAmplifier()));
			}
			if (!ins.endsWithin(20)) {
				ans = Component.translatable("potion.withDuration", ans,
						MobEffectUtil.formatDuration(ins, 1));
			}
			return ans.withStyle(mobeffect.getCategory().getTooltipFormatting());
		}));
	}
}
