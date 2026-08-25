package com.l2hostility_tweaks.client.config;

import net.minecraft.util.FastColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void parsesValidColorBoundariesAndWhitespace() {
        assertArrayEquals(new int[]{0, 0, 255, 1},
                ClientL2HConfig.parseColorSegment(" 0 , 0 , 255 , 1 "));
        assertArrayEquals(new int[]{Integer.MAX_VALUE, 255, 0, 255},
                ClientL2HConfig.parseColorSegment("2147483647,255,0,255"));
        assertArrayEquals(new int[]{0, 255, 128},
                ClientL2HConfig.parseRgb("0,255,128"));
    }

    @Test
    void rejectsMalformedAndOutOfRangeColors() {
        for (Object value : List.of(
                "100,256,0,0", "100,0,-1,0", "100,+1,2,3",
                "2147483648,1,2,3", "100,1,2", "100,1,2,3,4",
                "100,,2,3", "100,1.5,2,3", "100,0xFF,2,3")) {
            assertNull(ClientL2HConfig.parseColorSegment(value), String.valueOf(value));
        }
        assertNull(ClientL2HConfig.parseColorSegment(100));
        assertNull(ClientL2HConfig.parseRgb("256,0,0"));
        assertNull(ClientL2HConfig.parseRgb("1,2"));
        assertNotNull(ClientL2HConfig.parseRgb("255,255,255"));
    }

    @Test
    void skipsInvalidRuntimeSegmentsAndKeepsValidOrder() {
        List<int[]> parsed = ClientL2HConfig.parseColorSegments(List.of(
                "300,4,5,6", "200,256,0,0", "100,1,2,3"));

        assertEquals(2, parsed.size());
        assertArrayEquals(new int[]{100, 1, 2, 3}, parsed.get(0));
        assertArrayEquals(new int[]{300, 4, 5, 6}, parsed.get(1));
    }

    @Test
    void fallsBackWhenRuntimeDefaultColorIsInvalid() {
        assertEquals(FastColor.ARGB32.color(255, 170, 170, 170),
                ClientL2HConfig.parseDefaultColor("256,0,0"));
    }
}
