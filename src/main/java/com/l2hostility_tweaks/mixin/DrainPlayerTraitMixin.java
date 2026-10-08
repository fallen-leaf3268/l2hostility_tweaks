package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.PlayerTraitRules;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.highlevel.DrainTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DrainTrait.class, remap = false)
public class DrainPlayerTraitMixin {

    @Redirect(method = "postInit", at = @At(value = "INVOKE",
            target = "Ldev/xkmc/l2hostility/content/capability/mob/MobTraitCap;hasTrait(Ldev/xkmc/l2hostility/content/traits/base/MobTrait;)Z"),
            require = 1)
    private boolean l2fix$filterPlayerTraitCandidate(MobTraitCap cap, MobTrait trait,
                                                     LivingEntity entity, int drainLevel) {
        return cap.hasTrait(trait) || entity instanceof Player player && (drainLevel <= 0 ||
                !PlayerTraitRules.checkAddition(player, cap, trait, drainLevel, true).allowed());
    }
}
