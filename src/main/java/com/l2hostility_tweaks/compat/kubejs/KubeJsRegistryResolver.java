package com.l2hostility_tweaks.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.StringJoiner;

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

    static boolean validateNonNegative(String field, int value) {
        if (value >= 0) return true;
        LOGGER.error("Invalid {}: {}. Expected a non-negative value", field, value);
        return false;
    }

    static boolean validatePositive(String field, int value) {
        if (value >= 1) return true;
        LOGGER.error("Invalid {}: {}. Expected a positive value", field, value);
        return false;
    }

    static void requireValidTraitConfiguration(ResourceLocation traitId, String... errors) {
        StringJoiner details = new StringJoiner(", ");
        for (String error : errors) {
            if (error != null) details.add(error);
        }
        if (details.length() > 0) {
            throw new IllegalStateException("Invalid KubeJS trait " + traitId + ": " + details);
        }
    }
}
