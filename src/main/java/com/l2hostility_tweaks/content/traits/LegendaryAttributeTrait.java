package com.l2hostility_tweaks.content.traits;

import dev.xkmc.l2hostility.content.logic.TraitManager;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2library.util.math.MathHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class LegendaryAttributeTrait extends LegendaryTrait {

	public record AttributeEntry(String legacyName, String modifierName, Supplier<Attribute> attribute, DoubleSupplier factor,
								 AttributeModifier.Operation op) {}

	private final AttributeEntry[] entries;

	public LegendaryAttributeTrait(IntSupplier color, AttributeEntry... entries) {
		super(ChatFormatting.GOLD);
		this.entries = entries;
		this.color = color;
	}

	private final IntSupplier color;

	@Override
	public int getColor() {
		return color.getAsInt();
	}

	@Override
	public void initialize(LivingEntity le, int level) {
		for (var e : entries) {
			double factor = e.factor.getAsDouble();
			if (!hasFiniteAmount(factor, level)) continue;
			Attribute attribute = e.attribute.get();
			var instance = le.getAttribute(attribute);
			if (instance != null) {
				instance.removeModifier(MathHelper.getUUIDFromString(e.legacyName()));
			}
			TraitManager.addAttribute(le, attribute, e.modifierName(), factor * level, e.op());
		}
	}

	@Override
	public void addDetail(List<Component> list) {
		super.addDetail(list);
		for (var e : entries) {
			double val = e.factor.getAsDouble();
			if (val == 0 || !hasFiniteAmount(val, getMaxLevel())) continue;
			list.add(mapLevel(i -> Component.literal(formatAttributeValue(val * i, e.op()))
					.withStyle(ChatFormatting.AQUA)).append(CommonComponents.SPACE).append(
					Component.translatable(e.attribute.get().getDescriptionId()).withStyle(ChatFormatting.BLUE)));
		}
	}

	static boolean hasFiniteAmount(double factor, int level) {
		return Double.isFinite(factor) && Double.isFinite(factor * level);
	}

	static String formatAttributeValue(double amount, AttributeModifier.Operation operation) {
		return formatAttributeValue(amount, operation, ItemStack.ATTRIBUTE_MODIFIER_FORMAT);
	}

	static String formatAttributeValue(double amount, AttributeModifier.Operation operation, DecimalFormat format) {
		double displayValue = operation == AttributeModifier.Operation.ADDITION ? amount : amount * 100;
		String prefix = displayValue > 0 ? "+" : "";
		String suffix = operation == AttributeModifier.Operation.ADDITION ? "" : "%";
		return prefix + format.format(displayValue) + suffix;
	}
}
