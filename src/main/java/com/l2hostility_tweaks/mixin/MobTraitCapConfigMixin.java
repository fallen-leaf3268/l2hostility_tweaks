package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTraitCap.class, remap = false)
public class MobTraitCapConfigMixin {

    @Inject(method = "getConfigCache", at = @At("HEAD"), cancellable = true, require = 1)
    private void l2fix$useActiveNbtConfig(LivingEntity entity,
                                           CallbackInfoReturnable<EntityConfig.Config> cir) {
        EntityConfig.Config config = TraitGenerationHelper.selectActiveNbtConfig(entity);
        if (config != null) cir.setReturnValue(config);
    }
}
