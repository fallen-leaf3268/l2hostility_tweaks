package com.l2hostilityfix.mixin;

import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.L2Hostility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTraitCap.class, remap = false)
public class MobTraitCapMixin {

    private static final ResourceLocation NBT_CONDITION_ID = new ResourceLocation("l2hostilityfix", "nbt");

    @Shadow(remap = false)
    private EntityConfig.Config configCache;

    @Inject(method = "getConfigCache", at = @At("RETURN"), cancellable = true)
    private void checkNbtConditions(LivingEntity entity, CallbackInfoReturnable<EntityConfig.Config> cir) {
        EntityConfig merged = (EntityConfig) L2Hostility.ENTITY.getMerged();
        EntityConfig.Config nbtConfig = merged.get(entity.getType(), NBT_CONDITION_ID, LivingEntity.class, entity);
        if (nbtConfig != null) {
            configCache = nbtConfig;
            cir.setReturnValue(nbtConfig);
        }
    }
}
