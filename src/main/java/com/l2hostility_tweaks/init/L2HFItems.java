package com.l2hostility_tweaks.init;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.content.MiracleTwistedPocket;
import com.l2hostility_tweaks.content.RingItem;
import com.l2hostility_tweaks.content.TranquilBeltItem;
import com.l2hostility_tweaks.content.TraitSeal;
import com.l2hostility_tweaks.content.TraitUnloaderWand;
import com.l2hostility_tweaks.content.traits.SealTrait;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class L2HFItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "l2hostility_tweaks");

    public static final DeferredRegister<Item> L2H_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "l2hostility");

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "l2hostility_tweaks");

    private static final ResourceKey<net.minecraft.core.Registry<MobTrait>> TRAIT_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("l2hostility", "trait"));
    public static final DeferredRegister<MobTrait> TRAITS =
            DeferredRegister.create(TRAIT_KEY, "l2hostility");

    public static final RegistryObject<DimensionBreakerItem> DIMENSION_BREAKER;
    public static final RegistryObject<TraitSeal> TRAIT_SEAL;
    public static final RegistryObject<TraitUnloaderWand> TRAIT_UNLOADER;
    public static final RegistryObject<TranquilBeltItem> TRANQUIL_BELT;
    public static final RegistryObject<RingItem> DISPELL_RING;
    public static final RegistryObject<RingItem> DEMENTOR_RING;
    public static final RegistryObject<RingItem> ADAPTIVE_RING;
    public static final RegistryObject<RingItem> UNIFICATION_RING;

    public static final RegistryObject<MiracleTwistedPocket> MIRACLE_TWISTED_POCKET;
    public static final RegistryObject<TraitSymbol> SEAL_SYMBOL;

    public static final RegistryObject<CreativeModeTab> TAB;

    static {
        DIMENSION_BREAKER = ITEMS.register("dimension_breaker",
                () -> new DimensionBreakerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        TRAIT_SEAL = ITEMS.register("trait_seal",
                () -> new TraitSeal(new Item.Properties()));
        TRAIT_UNLOADER = ITEMS.register("trait_unloader_wand",
                () -> new TraitUnloaderWand(new Item.Properties()));
        TRANQUIL_BELT = ITEMS.register("tranquil_belt",
                () -> new TranquilBeltItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        DISPELL_RING = ITEMS.register("dispell_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 0.65f));
        DEMENTOR_RING = ITEMS.register("dementor_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 0.65f));
        ADAPTIVE_RING = ITEMS.register("adaptive_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 0.65f));
        UNIFICATION_RING = ITEMS.register("unification_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1.25f));
        MIRACLE_TWISTED_POCKET = ITEMS.register("miracle_twisted_pocket",
                () -> new MiracleTwistedPocket(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        SEAL_SYMBOL = L2H_ITEMS.register("seal",
                () -> new TraitSymbol(new Item.Properties()));
        TRAITS.register("seal",
                () -> new SealTrait(ChatFormatting.DARK_PURPLE));
        TAB = TABS.register("tab", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.l2hostility_tweaks"))
                .icon(() -> new ItemStack(TRAIT_SEAL.get()))
                .displayItems((params, output) -> {
                    output.accept(DIMENSION_BREAKER.get());
                    output.accept(TRAIT_SEAL.get());
                    output.accept(TRAIT_UNLOADER.get());
                    output.accept(TRANQUIL_BELT.get());
                    output.accept(DISPELL_RING.get());
                    output.accept(DEMENTOR_RING.get());
                    output.accept(ADAPTIVE_RING.get());
                    output.accept(UNIFICATION_RING.get());
                    output.accept(MIRACLE_TWISTED_POCKET.get());
                    for (int i = 1; i <= 5; i++)
                        output.accept(EnchantedBookItem.createForEnchantment(
                                new EnchantmentInstance(L2HFEnchantments.REPRINT_COUNTER.get(), i)));
                    for (int i = 1; i <= 3; i++)
                        output.accept(EnchantedBookItem.createForEnchantment(
                                new EnchantmentInstance(L2HFEnchantments.ABYSS_POCKET.get(), i)));
                    for (int i = 1; i <= 3; i++)
                        output.accept(EnchantedBookItem.createForEnchantment(
                                new EnchantmentInstance(L2HFEnchantments.GLUTTONY_POCKET.get(), i)));
                })
                .build());
    }

    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        L2H_ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TRAITS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
