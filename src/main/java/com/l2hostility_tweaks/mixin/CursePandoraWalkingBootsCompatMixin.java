package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.WalkingBootsModifierIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Pseudo
@Mixin(targets = "dev.xkmc.curseofpandora.content.reality.CursePandoraUtil", remap = false)
public abstract class CursePandoraWalkingBootsCompatMixin {

    @Redirect(
            method = "remove(Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;Ljava/util/UUID;Ljava/lang/String;Ljava/util/Set;Ldev/xkmc/curseofpandora/content/reality/CursePandoraUtil$ValueConsumer;Ldev/xkmc/curseofpandora/content/reality/CursePandoraUtil$ValueConsumer;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z",
                    remap = false),
            require = 0,
            remap = false)
    private static boolean l2fix$ignoreWalkingBootsCap(Set<?> ignored, Object candidate) {
        return l2fix$shouldIgnore(ignored, candidate);
    }

    private static boolean l2fix$shouldIgnore(Set<?> ignored, Object candidate) {
        return WalkingBootsModifierIds.isMovementSpeedCapModifier(candidate)
                || ignored.contains(candidate);
    }
}
