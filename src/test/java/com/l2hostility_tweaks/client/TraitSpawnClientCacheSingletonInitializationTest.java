package com.l2hostility_tweaks.client;

import org.junit.jupiter.api.Test;

import static com.l2hostility_tweaks.network.TraitSpawnIndexCodecTest.completeSnapshot;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TraitSpawnClientCacheSingletonInitializationTest {

    @Test
    void singletonCanInstallFirstNetworkSnapshot() {
        assertDoesNotThrow(() -> TraitSpawnClientCache.INSTANCE.install(completeSnapshot(1)));
    }
}
