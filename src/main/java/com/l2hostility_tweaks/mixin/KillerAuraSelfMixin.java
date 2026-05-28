package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.traits.legendary.KillerAuraTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = KillerAuraTrait.class, remap = false)
public class KillerAuraSelfMixin {

	private static final ThreadLocal<LivingEntity> l2fix$holder = new ThreadLocal<>();

	@Inject(method = "tick", at = @At("HEAD"))
	public void l2fix$captureHolder(LivingEntity mob, int level, CallbackInfo ci) {
		l2fix$holder.set(mob);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	public List<LivingEntity> l2fix$excludeSelf(Level level, Class<LivingEntity> cls, AABB box) {
		LivingEntity holder = l2fix$holder.get();
		return level.getEntitiesOfClass(cls, box).stream()
				.filter(e -> e != holder)
				.toList();
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Mob;getTarget()Lnet/minecraft/world/entity/LivingEntity;"))
	public LivingEntity l2fix$playerAttackingTarget(Mob entity) {
		LivingEntity holder = l2fix$holder.get();
		if (holder instanceof Player
				&& entity.getLastHurtByMob() == holder) {
			return holder;
		}
		return entity.getTarget();
	}
}
