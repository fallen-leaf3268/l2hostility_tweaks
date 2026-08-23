package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.legendary.KillerAuraTrait;
import dev.xkmc.l2hostility.init.data.LHDamageTypes;
import dev.xkmc.l2hostility.init.registrate.LHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.IntFunction;

@Mixin(value = KillerAuraTrait.class, remap = false)
public class KillerAuraTraitMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void l2fix$killerAuraTick(LivingEntity mob, int level, CallbackInfo ci) {
		ci.cancel();
		int itv = L2HConfig.getKillerAuraInterval(level);
		int damage = L2HConfig.getKillerAuraDamage(level);
		int range = dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraRange.get();
		if (!mob.level().isClientSide() && mob.tickCount % itv == 0) {
			MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
			AABB box = mob.getBoundingBox().inflate(range);
			for (var e : mob.level().getEntitiesOfClass(LivingEntity.class, box)) {
				boolean nonCreativePlayer = e instanceof Player player && !player.getAbilities().instabuild;
				boolean candidateTargetsHolder = e instanceof Mob candidateMob && candidateMob.getTarget() == mob;
				boolean holderTargetsCandidate = mob instanceof Mob holderMob && holderMob.getTarget() == e;
				boolean recentlyHitByPlayerHolder = mob instanceof Player && e instanceof Mob candidateMob &&
						candidateMob.getLastHurtByMob() == mob;
				if (!l2fix$shouldTarget(e == mob, nonCreativePlayer, candidateTargetsHolder,
						holderTargetsCandidate, recentlyHitByPlayerHolder)) {
					continue;
				}
				if (e.distanceTo(mob) > range) continue;
				if (LHItems.ABRAHADABRA.get().isOn(e)) continue;
				TraitEffectCache cache = new TraitEffectCache(e);
				cap.traitEvent((k, v) -> k.postHurtPlayer(v, mob, cache));
				e.hurt(new DamageSource(LHDamageTypes.forKey(mob.level(), LHDamageTypes.KILLER_AURA), null, mob), damage);
			}
		}
		if (mob.level().isClientSide()) {
			Vec3 center = mob.position();
			float tpi = (float) (Math.PI * 2);
			Vec3 v0 = new Vec3(0, range, 0);
			v0 = v0.xRot(tpi / 4).yRot(mob.getRandom().nextFloat() * tpi);
			mob.level().addAlwaysVisibleParticle(ParticleTypes.FLAME,
					center.x + v0.x, center.y + v0.y + 0.5f, center.z + v0.z, 0, 0, 0);
		}
	}

	@Unique
	static boolean l2fix$shouldTarget(boolean self, boolean nonCreativePlayer,
									 boolean candidateTargetsHolder, boolean holderTargetsCandidate,
									 boolean recentlyHitByPlayerHolder) {
		return !self && (nonCreativePlayer || candidateTargetsHolder ||
				holderTargetsCandidate || recentlyHitByPlayerHolder);
	}

	@Redirect(method = "addDetail", at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
	private boolean l2fix$killerAuraDetail(List<Component> list, Object component) {
		int max = ((KillerAuraTrait) (Object) this).getMaxLevel();
		return list.add(Component.translatable(((KillerAuraTrait) (Object) this).getDescriptionId() + ".desc",
				l2fix$mapLevel(i -> Component.literal(L2HConfig.getKillerAuraDamage(i) + "")
						.withStyle(ChatFormatting.AQUA), max),
				Component.literal("" + dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraRange.get()).withStyle(ChatFormatting.AQUA),
				l2fix$mapLevel(i -> Component.literal(
						String.format("%.1f", L2HConfig.getKillerAuraInterval(i) / 20.0))
						.withStyle(ChatFormatting.AQUA), max))
				.withStyle(ChatFormatting.GRAY));
	}

	private static Component l2fix$mapLevel(IntFunction<Component> func, int max) {
		Component comp = null;
		for (int i = 1; i <= max; i++) {
			Component part = func.apply(i);
			comp = comp == null ? part : comp.copy().append(Component.literal("/").withStyle(ChatFormatting.GRAY)).append(part);
		}
		return comp;
	}
}
