package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.config.EntityConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EntityConfig.Config.class, remap = false)
public interface ConfigAccessor {

    @Accessor("traits")
    java.util.ArrayList<?> getTraitsList();

    @Accessor("traits")
    void setTraitsList(java.util.ArrayList<?> traits);
}
