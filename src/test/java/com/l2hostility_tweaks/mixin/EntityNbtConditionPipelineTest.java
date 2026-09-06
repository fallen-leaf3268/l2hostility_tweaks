package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityNbtConditionPipelineTest {

    private static final Path CONFIG_MERGER = Path.of(
            "src/main/java/com/l2hostility_tweaks/mixin/ConfigMergerMixin.java");

    @Test
    void recordsDisplayMetadataForEveryJsonObjectButNbtOnlyForNbtEntries() throws IOException {
        String source = Files.readString(CONFIG_MERGER);

        assertTrue(source.contains("new ConfigEntry(configId, rawConfig"));
        assertTrue(source.contains("((EntityConfigDisplayData) (Object) ec.list.get(i))"));
        assertTrue(source.contains(".l2fix$setDisplayMetadata(configEntry.sourceId(), configEntry.rawConfig())"));
        assertTrue(source.contains("if (nbtElement == null)"));
        assertTrue(source.contains("new ConfigEntry(configId, rawConfig, EntityConfigNbtData.State.NONE, null)"));
        assertFalse(source.contains("if (nbtElement == null) continue;"));
    }
}
