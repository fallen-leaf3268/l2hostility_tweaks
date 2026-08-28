package com.l2hostility_tweaks.compat.kubejs;

import com.l2hostility_tweaks.content.traits.LegendaryAttributeTrait;
import dev.xkmc.l2hostility.compat.kubejs.AbstractTraitBuilder;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LegendaryAttributeTraitBuilder extends AbstractTraitBuilder<LegendaryAttributeTraitBuilder> {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:kubejs");
	private static final String VALID_OPERATIONS = "+, add, addition, %, +%, base, mult_base, " +
			"multiply_base, *, x, *%, x%, total, mult_total, multiply_total";
	private final List<LegendaryAttributeTrait.AttributeEntry> list = new ArrayList<>();

	public LegendaryAttributeTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendaryAttributeTraitBuilder attribute(String name, String attribute, double factor, String operation) {
		AttributeModifier.Operation op = l2fix$resolveOperation(operation);
		if (op == null) return this;
		var resolved = KubeJsRegistryResolver.resolve("attribute", attribute,
				ForgeRegistries.ATTRIBUTES::getValue);
		if (resolved == null) return this;
		list.add(new LegendaryAttributeTrait.AttributeEntry(
				name, () -> resolved,
				() -> factor, op
		));
		return this;
	}

	private static AttributeModifier.Operation l2fix$resolveOperation(String operation) {
		AttributeModifier.Operation resolved = operation == null ? null : switch (operation) {
			case "%", "+%", "base", "BASE", "mult_base", "MULT_BASE",
				 "multiply_base", "MULTIPLY_BASE" -> AttributeModifier.Operation.MULTIPLY_BASE;
			case "*", "x", "*%", "x%", "total", "TOTAL", "mult_total", "MULT_TOTAL",
				 "multiply_total", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
			case "+", "add", "ADD", "addition", "ADDITION" -> AttributeModifier.Operation.ADDITION;
			default -> null;
		};
		if (resolved == null) {
			LOGGER.error("Invalid attribute operation: {}. Expected one of: {}",
					operation, VALID_OPERATIONS);
		}
		return resolved;
	}

	@Override
	public MobTrait createObject() {
		if (color == null) color(ChatFormatting.GOLD);
		return new LegendaryAttributeTrait(color, list.toArray(LegendaryAttributeTrait.AttributeEntry[]::new));
	}
}
