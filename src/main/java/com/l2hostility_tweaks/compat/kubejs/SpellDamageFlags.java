package com.l2hostility_tweaks.compat.kubejs;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpellDamageFlags {

	private static final Set<TagKey<DamageType>> ACTIVE_TAGS = ConcurrentHashMap.newKeySet();

	public static void enableBypass(String type) {
		TagKey<DamageType> tag = switch (type) {
			case "bypass_armor" -> DamageTypeTags.BYPASSES_ARMOR;
			case "bypass_magic", "bypass_effects" -> DamageTypeTags.BYPASSES_EFFECTS;
			case "bypass_cooldown" -> DamageTypeTags.BYPASSES_COOLDOWN;
			case "bypass_resistance" -> DamageTypeTags.BYPASSES_RESISTANCE;
			case "bypass_enchantments" -> DamageTypeTags.BYPASSES_ENCHANTMENTS;
			default -> null;
		};
		if (tag != null) {
			ACTIVE_TAGS.add(tag);
		}
	}

	public static boolean isBypassEnabled(TagKey<DamageType> tag) {
		return ACTIVE_TAGS.contains(tag);
	}

	public static void clear() {
		ACTIVE_TAGS.clear();
	}
}
