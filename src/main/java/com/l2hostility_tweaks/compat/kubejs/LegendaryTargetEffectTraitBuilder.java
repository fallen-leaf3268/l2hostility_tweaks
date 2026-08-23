package com.l2hostility_tweaks.compat.kubejs;

import com.l2hostility_tweaks.content.traits.LegendaryTargetEffectTrait;
import com.l2hostility_tweaks.content.traits.LegendaryTraitColor;
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
		var mobEffect = KubeJsRegistryResolver.resolve("mob effect", effect,
				ForgeRegistries.MOB_EFFECTS::getValue);
		if (mobEffect == null) {
			this.func = i -> new MobEffectInstance(MobEffects.WEAKNESS, 100, i - 1);
		} else {
			this.func = i -> new MobEffectInstance(mobEffect, duration * i, amplifier);
		}
		return this;
	}

	public LegendaryTargetEffectTraitBuilder fixedDuration(String effect, int duration) {
		var mobEffect = KubeJsRegistryResolver.resolve("mob effect", effect,
				ForgeRegistries.MOB_EFFECTS::getValue);
		if (mobEffect == null) {
			this.func = i -> new MobEffectInstance(MobEffects.WEAKNESS, 100, i - 1);
		} else {
			this.func = i -> new MobEffectInstance(mobEffect, duration, i - 1);
		}
		return this;
	}

	@Override
	public MobTrait createObject() {
		if (func == null) func = i -> new MobEffectInstance(MobEffects.WEAKNESS, 100, i - 1);
		color = LegendaryTraitColor.normalize(color);
		return new LegendaryTargetEffectTrait(color, func);
	}
}
