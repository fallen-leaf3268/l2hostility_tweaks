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

        assertEquals(2, count(source, "ReprintDamageCalculator.calculate("));
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
}
