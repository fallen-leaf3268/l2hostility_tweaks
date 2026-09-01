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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LegendaryAttributeTraitBuilder extends AbstractTraitBuilder<LegendaryAttributeTraitBuilder> {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:kubejs");
	private static final String VALID_OPERATIONS = "+, add, addition, %, +%, base, mult_base, " +
			"multiply_base, *, x, *%, x%, total, mult_total, multiply_total";
	private final List<LegendaryAttributeTrait.AttributeEntry> list = new ArrayList<>();
	private final List<String> configurationErrors = new ArrayList<>();
	private final Set<String> configuredEntries = new HashSet<>();

	public LegendaryAttributeTraitBuilder(ResourceLocation id) {
		super(id);
	}

	public LegendaryAttributeTraitBuilder attribute(String name, String attribute, double factor, String operation) {
		if (!l2fix$isValidName(name)) {
			LOGGER.error("Invalid attribute modifier name: {}. Expected a non-blank name", name);
			configurationErrors.add("modifier name=" + name + " (expected non-blank)");
			return this;
		}
		if (!l2fix$isValidFactor(factor)) {
			LOGGER.error("Invalid attribute factor: {}. Expected a finite non-zero number", factor);
			configurationErrors.add("factor=" + factor + " (expected finite and non-zero)");
			return this;
		}
		AttributeModifier.Operation op = l2fix$resolveOperation(operation);
		if (op == null) {
			configurationErrors.add("operation=" + operation + " (expected one of: " + VALID_OPERATIONS + ")");
			return this;
		}
		var resolved = KubeJsRegistryResolver.resolve("attribute", attribute,
				key -> ForgeRegistries.ATTRIBUTES.getValue(key));
		if (resolved == null) {
			configurationErrors.add("attribute=" + attribute + " (invalid or unregistered)");
			return this;
		}
		ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(resolved);
		String entryKey = l2fix$entryKey(attributeId, name);
		if (!configuredEntries.add(entryKey)) {
			configurationErrors.add("duplicate attribute/name=" + attributeId + "/" + name);
			LOGGER.error("Duplicate legendary attribute/name combination: {}/{}", attributeId, name);
			return this;
		}
		list.add(new LegendaryAttributeTrait.AttributeEntry(
				name, l2fix$modifierName(id, name), () -> resolved,
				() -> factor, op
		));
		return this;
	}

	static boolean l2fix$isValidName(String name) {
		return name != null && !name.isBlank();
	}

	static boolean l2fix$isValidFactor(double factor) {
		return Double.isFinite(factor) && factor != 0;
	}

	static boolean l2fix$isFiniteFactor(double factor) {
		return l2fix$isValidFactor(factor);
	}

	static String l2fix$modifierName(ResourceLocation traitId, String name) {
		String id = traitId.toString();
		return "l2hostility_tweaks:kubejs/" + id.length() + ":" + id + "/" + name;
	}

	static String l2fix$entryKey(ResourceLocation attributeId, String name) {
		String id = attributeId.toString();
		return id.length() + ":" + id + "/" + name;
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
		List<String> errors = new ArrayList<>(configurationErrors);
		if (list.isEmpty()) errors.add("at least one attribute is required");
		KubeJsRegistryResolver.requireValidTraitConfiguration(id, errors.toArray(String[]::new));
		if (color == null) color(ChatFormatting.GOLD);
		return new LegendaryAttributeTrait(color, list.toArray(LegendaryAttributeTrait.AttributeEntry[]::new));
	}
}
