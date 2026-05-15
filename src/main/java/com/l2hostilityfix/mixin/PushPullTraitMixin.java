package com.l2hostilityfix.mixin;

import com.l2hostilityfix.util.ImmunityHelper;
import dev.xkmc.l2hostility.content.traits.legendary.PushPullTrait;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PushPullTrait.class, remap = true)
public class PushPullTraitMixin {

    @Redirect(method = "tick",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private void preventPushIfImmune(LivingEntity entity, double x, double y, double z) {
        if (ImmunityHelper.isImmuneToForce(entity)) {
            return;
        }
        entity.push(x, y, z);
    }
}
