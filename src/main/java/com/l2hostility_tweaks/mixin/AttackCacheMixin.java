package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AttackCache.class, remap = false)
public class AttackCacheMixin {

	@Redirect(method = "pushAttackPre", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;getEntity()Lnet/minecraft/world/entity/Entity;", remap = true))
	private Entity l2fix$getEntityInPushAttackPre(DamageSource source) {
		return ImmunityHelper.resolveLivingAttacker(source);
	}
}
