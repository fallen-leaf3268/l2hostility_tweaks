package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2damagetracker.contents.attack.CreateSourceEvent;
import dev.xkmc.l2damagetracker.init.data.L2DamageTypes;
import dev.xkmc.l2hostility.content.traits.legendary.DementorTrait;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DementorTrait.class, remap = false)
public class DementorTraitMixin {

	@Inject(method = "onAttackedByOthers", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$dementorDefense(int level, LivingEntity entity, LivingAttackEvent event, CallbackInfo ci) {
		var attacker = ImmunityHelper.resolveLivingAttacker(event.getSource());
		if (attacker != null && ImmunityHelper.hasCombatCurioWithTag(attacker, L2HFBypassTags.BYPASSES_DEMENTOR_ITEM)) {
			ci.cancel();
			return;
		}
		if (L2HConfig.isOldDementorEnabled()) {
			var source = event.getSource();
			if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ||
					source.is(DamageTypeTags.BYPASSES_EFFECTS) ||
					source.is(L2DamageTypes.MAGIC))
				return;
			event.setCanceled(true);
			ci.cancel();
		}
	}

	@Inject(method = "onCreateSource", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$dementorPlayerAttack(int level, LivingEntity attacker, CreateSourceEvent event, CallbackInfo ci) {
		if (attacker instanceof Player && L2HConfig.isOldDementorEnabled()) ci.cancel();
	}
}
