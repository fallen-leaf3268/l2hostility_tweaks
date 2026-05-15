package com.l2hostilityfix.init;

import com.l2hostilityfix.content.enchantment.AntiReprintEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class L2HFEnchantments {

	public static final DeferredRegister<Enchantment> REGISTRY =
		DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "l2hostilityfix");

	public static final RegistryObject<AntiReprintEnchantment> REPRINT_COUNTER =
		REGISTRY.register("reprint_counter", AntiReprintEnchantment::new);
}
