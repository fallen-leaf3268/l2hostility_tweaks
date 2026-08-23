package com.l2hostility_tweaks.client.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNull;

class ClientL2HConfigTest {

    @Test
    void invalidatesParsedColorSegments() throws Exception {
        Field field = ClientL2HConfig.class.getDeclaredField("parsedColorSegments");
        field.setAccessible(true);
        field.set(null, new ArrayList<>());

        ClientL2HConfig.invalidateCaches();

        assertNull(field.get(null));
    }
}
