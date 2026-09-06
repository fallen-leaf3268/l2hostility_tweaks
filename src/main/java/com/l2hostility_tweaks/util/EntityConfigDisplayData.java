package com.l2hostility_tweaks.util;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public interface EntityConfigDisplayData {

    void l2fix$setDisplayMetadata(ResourceLocation sourceId, JsonObject rawConfig);

    ResourceLocation l2fix$getSourceId();

    JsonObject l2fix$getRawConfig();
}
