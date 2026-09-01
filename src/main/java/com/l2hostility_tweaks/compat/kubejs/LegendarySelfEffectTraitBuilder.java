package com.l2hostility_tweaks.compat.kubejs;

import com.l2hostility_tweaks.content.traits.LegendarySelfEffectTrait;
import com.l2hostility_tweaks.content.traits.LegendaryTraitColor;
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
	private String effectConfigurationError;
	private String durationConfigurationError;
	private String amplifierConfigurationError;

	public LegendarySelfEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendarySelfEffectTraitBuilder effect(String id) {
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

	public LegendarySelfEffectTraitBuilder time(int ticks) {
		if (KubeJsRegistryResolver.validatePositive("legendary self effect duration", ticks)) {
			this.duration = ticks;
			this.durationConfigurationError = null;
		} else {
			this.durationConfigurationError = "duration=" + ticks;
		}
		return this;
	}

	public LegendarySelfEffectTraitBuilder effectLevel(int amp) {
		if (KubeJsRegistryResolver.validateNonNegative("legendary self effect amplifier per level", amp)) {
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
		color = LegendaryTraitColor.normalize(color);
		return new LegendarySelfEffectTrait(color, effect, duration, amplifierPerLevel);
	}
}
