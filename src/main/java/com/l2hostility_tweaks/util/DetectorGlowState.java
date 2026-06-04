package com.l2hostility_tweaks.util;

import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.util.List;

public class DetectorGlowState {

	private static final ResourceLocation DETECTOR_GLASSES_ID = new ResourceLocation("l2hostility", "detector_glasses");
	private static final String TAG_GLOW_DISABLED = "DetectorGlowDisabled";

	private static Item cachedGlasses;
	private static int cacheTick = -1;
	private static WeakReference<Player> cachedPlayerRef = new WeakReference<>(null);
	private static boolean cachedValue;

	public static boolean isGlowDisabled(Player player) {
		if (player.tickCount == cacheTick && player == cachedPlayerRef.get()) {
			return cachedValue;
		}
		cachedValue = readFromPlayer(player);
		cacheTick = player.tickCount;
		cachedPlayerRef = new WeakReference<>(player);
		return cachedValue;
	}

	private static boolean readFromPlayer(Player player) {
		Item glasses = getGlassesItem();
		if (glasses == null) return false;

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.is(glasses) && stack.hasTag() && stack.getTag().getBoolean(TAG_GLOW_DISABLED)) {
				return true;
			}
		}

		List<ItemStack> curioStacks = CurioCompat.getItems(player, s -> s.is(glasses));
		for (ItemStack stack : curioStacks) {
			if (stack.hasTag() && stack.getTag().getBoolean(TAG_GLOW_DISABLED)) {
				return true;
			}
		}

		return false;
	}

	private static Item getGlassesItem() {
		if (cachedGlasses == null) {
			cachedGlasses = ForgeRegistries.ITEMS.getValue(DETECTOR_GLASSES_ID);
		}
		return cachedGlasses;
	}
}
