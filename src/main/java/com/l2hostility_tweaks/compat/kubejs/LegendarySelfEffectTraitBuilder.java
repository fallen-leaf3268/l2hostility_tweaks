package com.l2hostility_tweaks.compat.kubejs;

import com.l2hostility_tweaks.content.traits.LegendarySelfEffectTrait;
import dev.xkmc.l2hostility.compat.kubejs.AbstractTraitBuilder;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class LegendarySelfEffectTraitBuilder extends AbstractTraitBuilder<LegendarySelfEffectTraitBuilder> {

	private Supplier<MobEffect> effect = () -> MobEffects.WEAKNESS;
	private int duration = -1;
	private int amplifierPerLevel = 1;

	public LegendarySelfEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendarySelfEffectTraitBuilder effect(String id) {
		var eff = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
		if (eff != null) this.effect = () -> eff;
		return this;
	}

	public LegendarySelfEffectTraitBuilder time(int ticks) {
		this.duration = ticks;
		return this;
	}

	public LegendarySelfEffectTraitBuilder effectLevel(int amp) {
		this.amplifierPerLevel = amp;
		return this;
	}

	@Override
	public MobTrait createObject() {
		return new LegendarySelfEffectTrait(color, effect, duration, amplifierPerLevel);
	}
}
