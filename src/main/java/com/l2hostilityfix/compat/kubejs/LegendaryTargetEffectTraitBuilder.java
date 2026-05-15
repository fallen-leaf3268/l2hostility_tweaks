package com.l2hostilityfix.compat.kubejs;

import com.l2hostilityfix.content.traits.LegendaryTargetEffectTrait;
import dev.xkmc.l2hostility.compat.kubejs.AbstractTraitBuilder;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public class LegendaryTargetEffectTraitBuilder extends AbstractTraitBuilder<LegendaryTargetEffectTraitBuilder> {

	private Function<Integer, MobEffectInstance> func;

	public LegendaryTargetEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendaryTargetEffectTraitBuilder fixedLevel(String effect, int duration, int amplifier) {
		this.func = i -> new MobEffectInstance(
				ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effect)),
				duration * i, amplifier);
		return this;
	}

	public LegendaryTargetEffectTraitBuilder fixedDuration(String effect, int duration) {
		this.func = i -> new MobEffectInstance(
				ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effect)),
				duration, i - 1);
		return this;
	}

	@Override
	public MobTrait createObject() {
		if (func == null) func = i -> new MobEffectInstance(MobEffects.WEAKNESS, 100, i - 1);
		return new LegendaryTargetEffectTrait(color, func);
	}
}
