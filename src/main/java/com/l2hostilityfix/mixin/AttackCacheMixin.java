package com.l2hostilityfix.mixin;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AttackCache.class, remap = false)
public class AttackCacheMixin {

	@Redirect(method = "pushAttackPre", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;getEntity()Lnet/minecraft/world/entity/Entity;", remap = true))
	private Entity l2fix$getEntityInPushAttackPre(DamageSource source) {
		Entity entity = source.getEntity();
		if (entity != null) return entity;
		Entity direct = source.getDirectEntity();
		if (direct instanceof LivingEntity le) return le;
		if (direct instanceof Projectile proj && proj.getOwner() instanceof LivingEntity owner) return owner;
		return null;
	}
}
