package com.l2hostility_tweaks.content.enchantment;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class GluttonyPocketEnchantment extends Enchantment {

	private static final EnchantmentCategory CATEGORY = EnchantmentCategory.create("L2HTWEAKS_GLUTTONY_POCKET",
			item -> BuiltInRegistries.ITEM.getKey(item).equals(new ResourceLocation("l2hostility", "pocket_of_restoration")));

	public GluttonyPocketEnchantment() {
		super(Rarity.RARE, CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean canEnchant(ItemStack stack) {
		return stack.is(BuiltInRegistries.ITEM.get(new ResourceLocation("l2hostility", "pocket_of_restoration")))
			|| stack.is(BuiltInRegistries.ITEM.get(new ResourceLocation("l2hostility_tweaks", "miracle_twisted_pocket")));
	}

	@Override
	public boolean isTreasureOnly() {
		return true;
	}

	@Override
	public boolean isTradeable() {
		return false;
	}

	@Override
	public boolean isDiscoverable() {
		return false;
	}

	@Override
	public Component getFullname(int lv) {
		MutableComponent name = Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.DARK_PURPLE);
		if (lv != 1 || this.getMaxLevel() != 1) {
			name = name.append(" ").append(Component.translatable("enchantment.level." + lv));
		}
		return name;
	}
}
