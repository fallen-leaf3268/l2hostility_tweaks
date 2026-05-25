package com.l2hostility_tweaks.init;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.content.RingItem;
import com.l2hostility_tweaks.content.TranquilBeltItem;
import com.l2hostility_tweaks.content.TraitSeal;
import com.l2hostility_tweaks.content.TraitUnloaderWand;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class L2HFItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "l2hostility_tweaks");

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "l2hostility_tweaks");

    public static final RegistryObject<DimensionBreakerItem> DIMENSION_BREAKER;
    public static final RegistryObject<TraitSeal> TRAIT_SEAL;
    public static final RegistryObject<TraitUnloaderWand> TRAIT_UNLOADER;
    public static final RegistryObject<TraitSymbol> SEAL_SYMBOL;
    public static final RegistryObject<TranquilBeltItem> TRANQUIL_BELT;
    public static final RegistryObject<RingItem> DISPELL_RING;
    public static final RegistryObject<RingItem> DEMENTOR_RING;
    public static final RegistryObject<RingItem> ADAPTIVE_RING;

    public static final RegistryObject<CreativeModeTab> TAB;

    static {
        DIMENSION_BREAKER = ITEMS.register("dimension_breaker",
                () -> new DimensionBreakerItem(new Item.Properties().stacksTo(1)));
        TRAIT_SEAL = ITEMS.register("trait_seal",
                () -> new TraitSeal(new Item.Properties()));
        TRAIT_UNLOADER = ITEMS.register("trait_unloader_wand",
                () -> new TraitUnloaderWand(new Item.Properties()));
        SEAL_SYMBOL = ITEMS.register("seal",
                () -> new TraitSymbol(new Item.Properties()));
        TRANQUIL_BELT = ITEMS.register("tranquil_belt",
                () -> new TranquilBeltItem(new Item.Properties().stacksTo(1)));
        DISPELL_RING = ITEMS.register("dispell_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1)));
        DEMENTOR_RING = ITEMS.register("dementor_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1)));
        ADAPTIVE_RING = ITEMS.register("adaptive_ring",
                () -> new RingItem(new Item.Properties().stacksTo(1)));

        TAB = TABS.register("tab", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.l2hostility_tweaks"))
                .icon(() -> new ItemStack(DIMENSION_BREAKER.get()))
                .displayItems((params, output) -> {
                    output.accept(DIMENSION_BREAKER.get());
                    output.accept(TRAIT_SEAL.get());
                    output.accept(TRAIT_UNLOADER.get());
                    output.accept(SEAL_SYMBOL.get());
                    output.accept(TRANQUIL_BELT.get());
                    output.accept(DISPELL_RING.get());
                    output.accept(DEMENTOR_RING.get());
                    output.accept(ADAPTIVE_RING.get());
                })
                .build());
    }

    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
