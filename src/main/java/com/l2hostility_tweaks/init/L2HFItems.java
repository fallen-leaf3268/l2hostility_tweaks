package com.l2hostility_tweaks.init;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.content.TraitSeal;
import com.l2hostility_tweaks.content.TraitUnloaderWand;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class L2HFItems {

	public static final DeferredRegister<Item> ITEMS =
		DeferredRegister.create(ForgeRegistries.ITEMS, "l2hostility_tweaks");

	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "l2hostility_tweaks");

	public static final RegistryObject<TraitUnloaderWand> TRAIT_UNLOADER =
		ITEMS.register("trait_unloader_wand",
			() -> new TraitUnloaderWand(new Item.Properties()));

	public static final RegistryObject<TraitSeal> TRAIT_SEAL =
		ITEMS.register("trait_seal",
			() -> new TraitSeal(new Item.Properties()));

	public static final RegistryObject<DimensionBreakerItem> DIMENSION_BREAKER =
		ITEMS.register("dimension_breaker",
			() -> new DimensionBreakerItem(new Item.Properties().stacksTo(1)));

	public static final RegistryObject<CreativeModeTab> MAIN_TAB =
		CREATIVE_TABS.register("main",
			() -> CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.l2hostility_tweaks"))
				.icon(() -> new ItemStack(TRAIT_SEAL.get()))
				.displayItems((params, output) -> {
					output.accept(DIMENSION_BREAKER.get());
					output.accept(TRAIT_SEAL.get());
					output.accept(TRAIT_UNLOADER.get());
					ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
					EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(L2HFEnchantments.REPRINT_COUNTER.get(), 1));
					output.accept(book);
					ItemStack book5 = new ItemStack(Items.ENCHANTED_BOOK);
					EnchantedBookItem.addEnchantment(book5, new EnchantmentInstance(L2HFEnchantments.REPRINT_COUNTER.get(), 5));
					output.accept(book5);
				})
				.build());
}
