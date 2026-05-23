package com.l2hostility_tweaks.content.enchantment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class AntiReprintEnchantment extends Enchantment {

	public AntiReprintEnchantment() {
		super(Rarity.UNCOMMON, EnchantmentCategory.BREAKABLE, EquipmentSlot.values());
	}

	@Override
	public int getMaxLevel() {
		return 5;
	}

	@Override
	public boolean canEnchant(ItemStack stack) {
		return true;
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
		MutableComponent name = Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.LIGHT_PURPLE);
		if (lv != 1 || this.getMaxLevel() != 1) {
			name = name.append(" ").append(Component.translatable("enchantment.level." + lv));
		}
		return name;
	}
}
