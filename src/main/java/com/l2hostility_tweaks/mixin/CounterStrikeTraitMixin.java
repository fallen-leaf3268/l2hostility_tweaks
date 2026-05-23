package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.goals.CounterStrikeTrait;
import dev.xkmc.l2hostility.init.data.LHConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CounterStrikeTrait.class, remap = false)
public class CounterStrikeTraitMixin {

	@Inject(method = "onHurtByOthers", at = @At("HEAD"), remap = false)
	private void l2fix$playerRecordAttacker(int level, LivingEntity le, LivingHurtEvent event, CallbackInfo ci) {
		if (!(le instanceof Player)) return;
		if (le.level().isClientSide()) return;
		var target = event.getSource().getEntity();
		if (!(target instanceof LivingEntity)) return;
		var data = MobTraitCap.HOLDER.get(le).getOrCreateData(
				((CounterStrikeTrait) (Object) this).getRegistryName(),
				CounterStrikeTrait.Data::new);
		data.strikeId = target.getUUID();
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$playerTick(LivingEntity le, int level, CallbackInfo ci) {
		if (!(le instanceof Player player)) return;
		if (le.level().isClientSide()) {
			ci.cancel();
			return;
		}
		var data = MobTraitCap.HOLDER.get(le).getOrCreateData(
				((CounterStrikeTrait) (Object) this).getRegistryName(),
				CounterStrikeTrait.Data::new);
		if (data.cooldown > 0) {
			data.cooldown--;
			ci.cancel();
			return;
		}
		if (!le.onGround()) {
			ci.cancel();
			return;
		}
		if (data.strikeId == null) {
			ci.cancel();
			return;
		}
		Entity target = ((ServerLevel) player.level()).getEntity(data.strikeId);
		if (target == null || !target.isAlive()) {
			ci.cancel();
			return;
		}
		Vec3 diff = target.position().subtract(le.position());
		diff = diff.normalize().scale(3);
		if (diff.y <= 0.2)
			diff = diff.add(0, 0.2, 0);
		le.setDeltaMovement(diff);
		le.hasImpulse = true;
		data.duration = LHConfig.COMMON.counterStrikeDuration.get();
		data.strikeId = null;
		ci.cancel();
	}
}
