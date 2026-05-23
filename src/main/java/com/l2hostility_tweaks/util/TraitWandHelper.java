package com.l2hostility_tweaks.util;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class TraitWandHelper {

	private static final String TRAIT_KEY = "l2hostility_trait";

	public static ItemStack setTrait(ItemStack stack, MobTrait trait) {
		stack.getOrCreateTag().putString(TRAIT_KEY, trait.getID());
		return stack;
	}

	public static MobTrait getTrait(ItemStack stack) {
		if (stack.getOrCreateTag().contains(TRAIT_KEY, Tag.TAG_STRING)) {
			String str = stack.getOrCreateTag().getString(TRAIT_KEY);
			ResourceLocation id = new ResourceLocation(str);
			MobTrait ans = LHTraits.TRAITS.get().getValue(id);
			if (ans != null) return ans;
		}
		return LHTraits.TRAITS.get().getValue(new ResourceLocation("l2hostility:tank"));
	}

	public static MobTrait nextTrait(MobTrait mod) {
		ArrayList<MobTrait> list = new ArrayList<>(LHTraits.TRAITS.get().getValues());
		int idx = list.indexOf(mod);
		if (idx + 1 < list.size()) {
			return list.get(idx + 1);
		}
		return list.get(0);
	}

	public static MobTrait prevTrait(MobTrait mod) {
		ArrayList<MobTrait> list = new ArrayList<>(LHTraits.TRAITS.get().getValues());
		int idx = list.indexOf(mod);
		if (idx != 0) {
			return list.get(idx - 1);
		}
		return list.get(list.size() - 1);
	}
}
