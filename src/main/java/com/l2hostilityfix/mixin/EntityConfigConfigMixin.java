package com.l2hostilityfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = dev.xkmc.l2hostility.content.config.EntityConfig.Config.class, remap = false)
public class EntityConfigConfigMixin {

    @Unique
    public Map<String, Object> nbt;
}
