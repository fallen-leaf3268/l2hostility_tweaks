package com.l2hostility_tweaks.init;

import com.l2hostility_tweaks.content.enchantment.AntiReprintEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class L2HFEnchantments {

	public static final DeferredRegister<Enchantment> REGISTRY =
		DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "l2hostility_tweaks");

	public static final RegistryObject<AntiReprintEnchantment> REPRINT_COUNTER =
		REGISTRY.register("reprint_counter", AntiReprintEnchantment::new);
}
