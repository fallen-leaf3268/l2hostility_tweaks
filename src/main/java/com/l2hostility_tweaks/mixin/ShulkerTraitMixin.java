package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.entity.BulletType;
import dev.xkmc.l2hostility.content.entity.HostilityBullet;
import dev.xkmc.l2hostility.content.traits.common.ShulkerTrait;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

@Mixin(value = ShulkerTrait.class, remap = false)
public abstract class ShulkerTraitMixin {

	@Shadow
	private IntSupplier interval;

	@Shadow
	private BulletType type;

	@Shadow
	private int offset;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void l2fix$playerTick(LivingEntity e, int level, CallbackInfo ci) {
		if (!(e instanceof Player player)) return;
		ci.cancel();
		if (e.level().isClientSide()) return;

		var cap = MobTraitCap.HOLDER.get(e);
		if (cap == null || cap.traits.isEmpty()) return;

		var data = cap.getOrCreateData(
				((ShulkerTrait) (Object) this).getRegistryName(),
				ShulkerTrait.Data::new);

		if (data.uuid != null &&
				e.level() instanceof ServerLevel sl &&
				sl.getEntity(data.uuid) instanceof ShulkerBullet)
			return;

		int intervalVal = interval.getAsInt();

		data.tickCount++;
		if (data.tickCount < intervalVal) return;
		if ((e.tickCount + offset) % intervalVal != 0) return;

		LivingEntity target = l2fix$findTargetInCone(player);
		if (target == null) return;

		var bullet = new HostilityBullet(e.level(), e, target,
				Direction.Axis.Y, type, level);
		data.tickCount = 0;
		if (type.limit())
			data.uuid = bullet.getUUID();
		e.level().addFreshEntity(bullet);
		e.playSound(SoundEvents.SHULKER_SHOOT, 2.0F,
				(e.getRandom().nextFloat() - e.getRandom().nextFloat()) * 0.2F + 1.0F);
	}

	private static LivingEntity l2fix$findTargetInCone(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();

		double range = 20.0;
		AABB box = player.getBoundingBox().inflate(range);

		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;

		for (Entity entity : player.level().getEntities(player, box,
				e -> e instanceof LivingEntity && e.isAlive()
						&& EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e))) {
			boolean ownedByPlayer = entity instanceof OwnableEntity ownable
					&& player.getUUID().equals(ownable.getOwnerUUID());
			if (l2fix$isFriendlyCandidate(
					entity.isAlliedTo(player), player.isAlliedTo(entity), ownedByPlayer)) continue;

			Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eye);
			double dist = toEntity.length();
			if (dist > range) continue;

			toEntity = toEntity.normalize();
			double dot = toEntity.dot(look);

			if (dot < 0.3) continue;

			double score = dist / (dot + 0.1);
			if (score < bestScore) {
				bestScore = score;
				best = (LivingEntity) entity;
			}
		}
		return best;
	}

	@Unique
	static boolean l2fix$isFriendlyCandidate(boolean candidateAlliedToPlayer,
			boolean playerAlliedToCandidate, boolean ownedByPlayer) {
		return candidateAlliedToPlayer || playerAlliedToCandidate || ownedByPlayer;
	}
}
