package com.l2hostility_tweaks.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSymbolMixinTest {

    @Test
    void loadsSealedUpgradeMixinOnDedicatedServers() throws IOException {
        var config = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/l2hostility_tweaks.mixins.json"))).getAsJsonObject();

        assertTrue(config.getAsJsonArray("mixins").contains(JsonParser.parseString("\"TraitSymbolMixin\"")));
        assertFalse(config.getAsJsonArray("client").contains(JsonParser.parseString("\"TraitSymbolMixin\"")));
    }

    @Test
    void displaysInvalidTraitIdAsLiteralText() {
        assertEquals("Invalid Trait ID",
                TraitSymbolMixin.l2fix$getTraitName(null, "Invalid Trait ID").getString());
    }

    @Test
    void preservesPreRefreshHealthPercentage() {
        assertEquals(30.0f, TraitSymbolMixin.l2fix$scaledHealth(60.0f, 100.0f, 50.0f), 0.0001f);
    }

    @Test
    void clampsRestoredHealthToExistingBounds() {
        assertEquals(50.0f, TraitSymbolMixin.l2fix$scaledHealth(120.0f, 100.0f, 50.0f));
        assertEquals(1.0f, TraitSymbolMixin.l2fix$scaledHealth(0.0f, 100.0f, 50.0f));
    }
}
