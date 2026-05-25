package com.l2hostility_tweaks.init;

import com.l2hostility_tweaks.content.traits.SealTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class L2HFTraits {

	public static final DeferredRegister<MobTrait> TRAITS =
			DeferredRegister.create(LHTraits.TRAITS.key(), "l2hostility_tweaks");

	public static final RegistryObject<SealTrait> SEAL_TRAIT;

	static {
		SEAL_TRAIT = TRAITS.register("seal", () -> new SealTrait(ChatFormatting.DARK_PURPLE));
	}

	public static void register() {
		TRAITS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}
