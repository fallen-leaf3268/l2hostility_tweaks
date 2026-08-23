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

	public LegendarySelfEffectTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendarySelfEffectTraitBuilder effect(String id) {
		var eff = KubeJsRegistryResolver.resolve("mob effect", id, ForgeRegistries.MOB_EFFECTS::getValue);
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
		color = LegendaryTraitColor.normalize(color);
		return new LegendarySelfEffectTrait(color, effect, duration, amplifierPerLevel);
	}
}
