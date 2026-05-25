package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.CreateSourceEvent;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2damagetracker.init.data.L2DamageTypes;
import dev.xkmc.l2hostility.content.traits.legendary.DispellTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = DispellTrait.class, remap = false)
public class DispellTraitMixin {

	@Inject(method = "onDamaged", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$dispellDefense(int level, LivingEntity entity, AttackCache cache, CallbackInfo ci) {
		var event = cache.getLivingDamageEvent();
		if (event == null) return;
		var attacker = event.getSource().getEntity();
		if (attacker instanceof LivingEntity living && ImmunityHelper.hasCurioWithTag(living, L2HFBypassTags.BYPASSES_DISPELL_ITEM)) {
			ci.cancel();
			return;
		}
		if (L2HConfig.isOldDispellEnabled()) {
			var source = event.getSource();
			if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ||
					source.is(DamageTypeTags.BYPASSES_EFFECTS) ||
					!source.is(L2DamageTypes.MAGIC))
				return;
			cache.addDealtModifier(DamageModifier.multTotal(0));
		}
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$dispellDetail(List<Component> list, CallbackInfo ci) {
		if (!L2HConfig.isOldDispellEnabled()) return;
		list.add(Component.translatable("trait.l2hostility_tweaks.dispell.immunity")
				.withStyle(ChatFormatting.GOLD));
	}

	@Inject(method = "onCreateSource", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$dispellPlayerAttack(int level, LivingEntity attacker, CreateSourceEvent event, CallbackInfo ci) {
		if (attacker instanceof Player) ci.cancel();
	}
}
