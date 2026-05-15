package com.l2hostilityfix;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class L2HFBypassTags {

	public static final TagKey<DamageType> BYPASSES_ADAPTIVE =
			TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("l2hostilityfix", "bypasses_adaptive"));
	public static final TagKey<DamageType> BYPASSES_DISPELL =
			TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("l2hostilityfix", "bypasses_dispell"));
	public static final TagKey<DamageType> BYPASSES_DEMENTOR =
			TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("l2hostilityfix", "bypasses_dementor"));
}
