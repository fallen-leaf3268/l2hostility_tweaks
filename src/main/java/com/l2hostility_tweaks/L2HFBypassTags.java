package com.l2hostility_tweaks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class L2HFBypassTags {

	public static final TagKey<Item> BYPASSES_ADAPTIVE_ITEM =
			TagKey.create(Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "bypasses_adaptive"));
	public static final TagKey<Item> BYPASSES_DISPELL_ITEM =
			TagKey.create(Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "bypasses_dispell"));
	public static final TagKey<Item> BYPASSES_DEMENTOR_ITEM =
			TagKey.create(Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "bypasses_dementor"));

	public static final TagKey<Block> ANTIBUILD_IMMUNE =
			TagKey.create(Registries.BLOCK, new ResourceLocation("l2hostility_tweaks", "antibuild_immune"));

	public static final TagKey<Item> ANTIBUILD_BYPASS =
			TagKey.create(Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "antibuild_bypass"));
}
