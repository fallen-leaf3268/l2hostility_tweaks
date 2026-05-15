package com.l2hostilityfix.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.xkmc.l2hostility.compat.kubejs.LHKJSPlugin;

public class L2HFKJSPlugin extends KubeJSPlugin {

	@Override
	public void init() {
		var traits = LHKJSPlugin.TRAITS.get();
		traits.addType("self_effect", SelfEffectTraitBuilder.class, SelfEffectTraitBuilder::new);
		traits.addType("legendary_attribute", LegendaryAttributeTraitBuilder.class, LegendaryAttributeTraitBuilder::new);
		traits.addType("legendary_self_effect", LegendarySelfEffectTraitBuilder.class, LegendarySelfEffectTraitBuilder::new);
		traits.addType("legendary_effect", LegendaryTargetEffectTraitBuilder.class, LegendaryTargetEffectTraitBuilder::new);
	}

	@Override
	public void registerBindings(BindingsEvent event) {
		event.add("L2HFix", SpellDamageFlags.class);
	}
}
