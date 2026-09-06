package com.l2hostility_tweaks.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l2hostility_tweaks.util.EntityConfigDisplayData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityConfigDisplayMetadataTest {

    @Test
    void configMixinDefensivelyCopiesDisplayMetadata() {
        EntityConfigDisplayData data = new EntityConfigConfigMixin();
        JsonObject raw = JsonParser.parseString("{\"presetTraitsOnly\":true}").getAsJsonObject();
        ResourceLocation source = new ResourceLocation("example", "boss");

        data.l2fix$setDisplayMetadata(source, raw);
        raw.addProperty("presetTraitsOnly", false);

        assertEquals(source, data.l2fix$getSourceId());
        assertTrue(data.l2fix$getRawConfig().get("presetTraitsOnly").getAsBoolean());
        JsonObject returned = data.l2fix$getRawConfig();
        returned.addProperty("presetTraitsOnly", false);
        assertTrue(data.l2fix$getRawConfig().get("presetTraitsOnly").getAsBoolean());
    }
}
