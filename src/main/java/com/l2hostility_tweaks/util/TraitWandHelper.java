package com.l2hostility_tweaks.util;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class TraitWandHelper {

	private static final String TRAIT_KEY = "l2hostility_trait";

	public static ItemStack setTrait(ItemStack stack, MobTrait trait) {
		stack.getOrCreateTag().putString(TRAIT_KEY, trait.getID());
		return stack;
	}

	public static boolean giveOrDrop(Player player, ItemStack stack) {
		return deliver(() -> player.addItem(stack), () -> !stack.isEmpty(),
				() -> player.drop(stack, false) != null);
	}

	public static boolean giveOrDrop(Player player, Item item, int totalCount) {
		boolean delivered = true;
		for (int count : splitCounts(totalCount, item.getMaxStackSize())) {
			delivered &= giveOrDrop(player, new ItemStack(item, count));
		}
		return delivered;
	}

	static List<Integer> splitCounts(int totalCount, int maxStackSize) {
		if (totalCount <= 0 || maxStackSize <= 0) return List.of();
		int fullStacks = totalCount / maxStackSize;
		int remainder = totalCount % maxStackSize;
		List<Integer> counts = new ArrayList<>(fullStacks + (remainder == 0 ? 0 : 1));
		for (int i = 0; i < fullStacks; i++) {
			counts.add(maxStackSize);
		}
		if (remainder > 0) counts.add(remainder);
		return counts;
	}

	static boolean deliver(Runnable insert, BooleanSupplier hasRemainder,
			BooleanSupplier drop) {
		insert.run();
		return !hasRemainder.getAsBoolean() || drop.getAsBoolean();
	}

	public static MobTrait getTrait(ItemStack stack) {
		if (stack.getOrCreateTag().contains(TRAIT_KEY, Tag.TAG_STRING)) {
			String str = stack.getOrCreateTag().getString(TRAIT_KEY);
			ResourceLocation id = parseTraitId(str);
			if (id != null) {
				MobTrait ans = LHTraits.TRAITS.get().getValue(id);
				if (ans != null) return ans;
			}
		}
		return LHTraits.TRAITS.get().getValue(new ResourceLocation("l2hostility:tank"));
	}

	static ResourceLocation parseTraitId(String value) {
		return ResourceLocation.tryParse(value);
	}

	public static MobTrait nextTrait(MobTrait mod) {
		ArrayList<MobTrait> list = new ArrayList<>(LHTraits.TRAITS.get().getValues());
		int idx = list.indexOf(mod);
		if (idx < 0) return mod;
		if (idx + 1 < list.size()) {
			return list.get(idx + 1);
		}
		return list.get(0);
	}

	public static MobTrait prevTrait(MobTrait mod) {
		ArrayList<MobTrait> list = new ArrayList<>(LHTraits.TRAITS.get().getValues());
		int idx = list.indexOf(mod);
		if (idx < 0) return mod;
		if (idx != 0) {
			return list.get(idx - 1);
		}
		return list.get(list.size() - 1);
	}
}
