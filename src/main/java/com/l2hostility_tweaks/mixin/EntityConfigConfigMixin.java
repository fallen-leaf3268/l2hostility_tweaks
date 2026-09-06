package com.l2hostility_tweaks.mixin;

import com.google.gson.JsonObject;
import com.l2hostility_tweaks.util.EntityConfigDisplayData;
import com.l2hostility_tweaks.util.EntityConfigNbtData;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(value = dev.xkmc.l2hostility.content.config.EntityConfig.Config.class, remap = false)
public class EntityConfigConfigMixin implements EntityConfigNbtData, EntityConfigDisplayData {

    @Unique
    private State l2fix$nbtConditionState = State.NONE;

    @Unique
    private JsonObject l2fix$nbtCondition;

    @Unique
    private ResourceLocation l2fix$sourceId;

    @Unique
    private JsonObject l2fix$rawConfig;

    @Override
    public void l2fix$setNbtCondition(State state, JsonObject condition) {
        State checkedState = Objects.requireNonNull(state);
        if (checkedState == State.VALID) {
            l2fix$nbtCondition = Objects.requireNonNull(condition);
        } else {
            l2fix$nbtCondition = null;
        }
        l2fix$nbtConditionState = checkedState;
    }

    @Override
    public State l2fix$getNbtConditionState() {
        return l2fix$nbtConditionState;
    }

    @Override
    public JsonObject l2fix$getNbtCondition() {
        return l2fix$nbtCondition;
    }

    @Override
    public void l2fix$setDisplayMetadata(ResourceLocation sourceId, JsonObject rawConfig) {
        l2fix$sourceId = sourceId;
        l2fix$rawConfig = Objects.requireNonNull(rawConfig).deepCopy();
    }

    @Override
    public ResourceLocation l2fix$getSourceId() {
        return l2fix$sourceId;
    }

    @Override
    public JsonObject l2fix$getRawConfig() {
        return l2fix$rawConfig == null ? null : l2fix$rawConfig.deepCopy();
    }
}
