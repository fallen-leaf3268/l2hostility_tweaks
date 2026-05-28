package com.l2hostility_tweaks.init;

import com.l2hostility_tweaks.content.enchantment.AbyssPocketEnchantment;
import com.l2hostility_tweaks.content.enchantment.AntiReprintEnchantment;
import com.l2hostility_tweaks.content.enchantment.GluttonyPocketEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class L2HFEnchantments {

	public static final DeferredRegister<Enchantment> REGISTRY =
		DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "l2hostility_tweaks");

	public static final RegistryObject<AntiReprintEnchantment> REPRINT_COUNTER =
		REGISTRY.register("reprint_counter", AntiReprintEnchantment::new);

	public static final RegistryObject<AbyssPocketEnchantment> ABYSS_POCKET =
		REGISTRY.register("abyss_pocket", AbyssPocketEnchantment::new);

	public static final RegistryObject<GluttonyPocketEnchantment> GLUTTONY_POCKET =
		REGISTRY.register("gluttony_pocket", GluttonyPocketEnchantment::new);

}
