package com.l2hostility_tweaks.compat.kubejs;

import com.l2hostility_tweaks.content.traits.LegendaryTargetEffectTrait;
import com.l2hostility_tweaks.content.traits.LegendaryTraitColor;
import dev.xkmc.l2hostility.compat.kubejs.AbstractTraitBuilder;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class LegendaryTargetEffectTraitBuilder extends AbstractTraitBuilder<LegendaryTargetEffectTraitBuilder> {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:kubejs");
	private Function<Integer, MobEffectInstance> func;
	private String configurationError;

	public LegendaryTargetEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendaryTargetEffectTraitBuilder fixedLevel(String effect, int duration, int amplifier) {
		if (!l2fix$isValidDuration(duration) || !l2fix$isValidAmplifier(amplifier)) {
			configurationError = "duration=" + duration + ", amplifier=" + amplifier;
			LOGGER.error("Invalid legendary target effect duration/amplifier: {}/{}. " +
					"Expected duration >= 1 and amplifier >= 0", duration, amplifier);
			return this;
		}
		var mobEffect = KubeJsRegistryResolver.resolve("mob effect", effect,
				key -> ForgeRegistries.MOB_EFFECTS.getValue(key));
		if (mobEffect != null) {
			this.func = i -> new MobEffectInstance(mobEffect, l2fix$saturatingDuration(duration, i), amplifier);
			this.configurationError = null;
		} else {
			this.configurationError = "effect=" + effect;
		}
		return this;
	}

	public LegendaryTargetEffectTraitBuilder fixedDuration(String effect, int duration) {
		if (!l2fix$isValidDuration(duration)) {
			configurationError = "duration=" + duration;
			LOGGER.error("Invalid legendary target effect duration: {}. Expected duration >= 1", duration);
			return this;
		}
		var mobEffect = KubeJsRegistryResolver.resolve("mob effect", effect,
				key -> ForgeRegistries.MOB_EFFECTS.getValue(key));
		if (mobEffect != null) {
			this.func = i -> new MobEffectInstance(mobEffect, duration, i - 1);
			this.configurationError = null;
		} else {
			this.configurationError = "effect=" + effect;
		}
		return this;
	}

	static boolean l2fix$isValidDuration(int duration) {
		return duration >= 1;
	}

	static boolean l2fix$isValidAmplifier(int amplifier) {
		return amplifier >= 0;
	}

	static int l2fix$saturatingDuration(int duration, int level) {
		long scaled = (long) duration * level;
		return scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
	}

	@Override
	public MobTrait createObject() {
		KubeJsRegistryResolver.requireValidTraitConfiguration(id, configurationError);
		if (func == null) func = i -> new MobEffectInstance(MobEffects.WEAKNESS, 100, i - 1);
		color = LegendaryTraitColor.normalize(color);
		return new LegendaryTargetEffectTrait(color, func);
	}
}
