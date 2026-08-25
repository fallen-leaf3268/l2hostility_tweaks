package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReprintTraitMixinIntegrationTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/l2hostility_tweaks/mixin/ReprintTraitMixin.java");

    @Test
    void bothReprintPathsUseSharedCalculator() throws IOException {
        String source = Files.readString(SOURCE);
        String mobPath = section(source,
                "private void l2fix$handleReprint(",
                "private void l2fix$playerReprint(");
        String playerPath = section(source,
                "private void l2fix$playerReprint(",
                "private void l2fix$addLinearInfo(");

        assertEquals(1, count(mobPath, "ReprintDamageCalculator.calculate(linear, points)"));
        assertEquals(1, count(playerPath, "ReprintDamageCalculator.calculate(linear, points)"));
    }

    @Test
    void removesAmbiguousSentinelAndPerCallInstanceState() throws IOException {
        String source = Files.readString(SOURCE);

        assertFalse(source.contains("total != -1"));
        assertFalse(source.contains("private int l2fix$antiReprintTotal"));
        assertFalse(source.contains("private int l2fix$antiReprintArmor"));
        assertFalse(source.contains("private boolean l2fix$linear"));
        assertFalse(source.contains("private boolean l2fix$hasCounter"));
    }

    private static int count(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Missing method marker");
        }
        return source.substring(start, end);
    }
}
