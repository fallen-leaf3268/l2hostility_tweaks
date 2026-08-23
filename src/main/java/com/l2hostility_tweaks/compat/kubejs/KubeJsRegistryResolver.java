package com.l2hostility_tweaks.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public final class KubeJsRegistryResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:kubejs");

    private KubeJsRegistryResolver() {
    }

    public static <T> T resolve(String registryType, String id, Function<ResourceLocation, T> lookup) {
        ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
        if (location == null) {
            LOGGER.error("Invalid {} id: {}", registryType, id);
            return null;
        }
        T value = lookup.apply(location);
        if (value == null) {
            LOGGER.error("Unknown {}: {}", registryType, id);
        }
        return value;
    }
}
