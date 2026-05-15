package com.l2hostilityfix.compat.kubejs;

import com.l2hostilityfix.content.traits.LegendaryAttributeTrait;
import dev.xkmc.l2hostility.compat.kubejs.AbstractTraitBuilder;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class LegendaryAttributeTraitBuilder extends AbstractTraitBuilder<LegendaryAttributeTraitBuilder> {

	private final List<LegendaryAttributeTrait.AttributeEntry> list = new ArrayList<>();

	public LegendaryAttributeTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendaryAttributeTraitBuilder attribute(String name, String attribute, double factor, String operation) {
		AttributeModifier.Operation op = switch (operation) {
			case "%", "+%", "base", "BASE", "mult_base", "MULT_BASE",
				 "multiply_base", "MULTIPLY_BASE" -> AttributeModifier.Operation.MULTIPLY_BASE;
			case "*", "x", "*%", "x%", "total", "TOTAL", "mult_total", "MULT_TOTAL",
				 "multiply_total", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
			default -> AttributeModifier.Operation.ADDITION;
		};
		list.add(new LegendaryAttributeTrait.AttributeEntry(
				name, () -> ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attribute)),
				() -> factor, op
		));
		return this;
	}

	@Override
	public MobTrait createObject() {
		if (color == null) color(ChatFormatting.GOLD);
		return new LegendaryAttributeTrait(color, list.toArray(LegendaryAttributeTrait.AttributeEntry[]::new));
	}
}
