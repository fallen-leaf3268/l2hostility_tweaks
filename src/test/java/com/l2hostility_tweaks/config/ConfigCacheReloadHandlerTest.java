package com.l2hostility_tweaks.config;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigCacheReloadHandlerTest {

    @Test
    void routesInvalidationByExactConfigSpec() throws Exception {
        Field commonCache = L2HConfig.class.getDeclaredField("parsedLevelThresholds");
        Field clientCache = ClientL2HConfig.class.getDeclaredField("parsedColorSegments");
        commonCache.setAccessible(true);
        clientCache.setAccessible(true);

        try {
            commonCache.set(null, new ArrayList<>());
            clientCache.set(null, new ArrayList<>());
            ConfigCacheReloadHandler.invalidate(L2HConfig.SPEC);
            assertNull(commonCache.get(null));
            assertNotNull(clientCache.get(null));

            commonCache.set(null, new ArrayList<>());
            clientCache.set(null, new ArrayList<>());
            ConfigCacheReloadHandler.invalidate(ClientL2HConfig.CLIENT_SPEC);
            assertNotNull(commonCache.get(null));
            assertNull(clientCache.get(null));
        } finally {
            L2HConfig.invalidateCaches();
            ClientL2HConfig.invalidateCaches();
        }
    }
}
