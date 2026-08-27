package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.highlevel.ArenaTrait;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArenaTrait.class, remap = false)
public class ArenaTraitMixin {

	@Inject(method = "onAttackedByOthers", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$playerBypass(int level, LivingEntity entity, LivingAttackEvent event, CallbackInfo ci) {
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
		if (event.getSource().getEntity() instanceof Player player
				&& MobTraitCap.HOLDER.isProper(player)) {
			if (MobTraitCap.HOLDER.get(player).getTraitLevel((ArenaTrait) (Object) this) >= level) {
				ci.cancel();
			}
		}
	}

	@Redirect(method = "onDamaged", at = @At(value = "INVOKE",
			target = "Ldev/xkmc/l2damagetracker/contents/attack/AttackCache;addDealtModifier(Ldev/xkmc/l2damagetracker/contents/attack/DamageModifier;)V"),
			remap = false)
	private void l2fix$skipReduction(AttackCache cache, DamageModifier modifier,
			int level, LivingEntity mob, AttackCache originalCache) {
		boolean bypass = cache.getAttacker() instanceof Player attacker
				&& MobTraitCap.HOLDER.isProper(attacker)
				&& MobTraitCap.HOLDER.get(attacker).getTraitLevel((ArenaTrait) (Object) this) >= level;
		if (!bypass) {
			cache.addDealtModifier(modifier);
		}
	}
}
