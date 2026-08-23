package com.l2hostility_tweaks.config;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import net.minecraftforge.fml.config.IConfigSpec;

public final class ConfigCacheReloadHandler {

    private ConfigCacheReloadHandler() {
    }

    public static void invalidate(IConfigSpec<?> spec) {
        if (spec == L2HConfig.SPEC) {
            L2HConfig.invalidateCaches();
        } else if (spec == ClientL2HConfig.CLIENT_SPEC) {
            ClientL2HConfig.invalidateCaches();
        }
    }
}
