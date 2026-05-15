package com.l2hostilityfix;

import com.l2hostilityfix.client.config.ClientL2HConfig;
import com.l2hostilityfix.config.L2HConfig;
import com.l2hostilityfix.init.L2HFEnchantments;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("l2hostilityfix")
public class L2HostilityFix {
    public L2HostilityFix() {
        L2HConfig.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientL2HConfig.CLIENT_SPEC, "l2_configs/l2hostilityfix-client.toml");
        L2HFEnchantments.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
