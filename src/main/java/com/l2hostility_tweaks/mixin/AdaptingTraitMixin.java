package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.common.AdaptingTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AdaptingTrait.class, remap = false)
public class AdaptingTraitMixin {

	@Inject(method = "onDamaged", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$adaptiveAdditiveReduction(int level, LivingEntity entity, AttackCache cache, CallbackInfo ci) {
		var event = cache.getLivingDamageEvent();
		if (event != null) {
			var attacker = event.getSource().getEntity();
			if (attacker instanceof LivingEntity living && ImmunityHelper.hasCombatCurioWithTag(living, L2HFBypassTags.BYPASSES_ADAPTIVE_ITEM)) {
				ci.cancel();
				return;
			}
		}

		if (!L2HConfig.isAdaptiveLinearEnabled()) return;

		ci.cancel();

		if (event == null) return;

		DamageSource source = event.getSource();
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_EFFECTS)) return;

		AdaptingTrait self = (AdaptingTrait) (Object) this;
		var cap = MobTraitCap.HOLDER.get(entity);
		var data = (AdaptingTrait.Data) cap.getOrCreateData(self.getRegistryName(), AdaptingTrait.Data::new);

		String id = source.getMsgId();
		double reductionPerStack = L2HConfig.getAdaptiveReductionPerStack();
		double maxReduction = L2HConfig.getAdaptiveMaxReduction();
		if (reductionPerStack <= 0) return;
		int maxStacks = (int) Math.ceil(maxReduction / reductionPerStack);

		if (data.memory.contains(id)) {
			int current = data.adaption.getOrDefault(id, 0);
			int next = Math.min(current + 1, maxStacks);
			data.adaption.put(id, next);
		} else {
			if (data.memory.size() >= level) {
				int index = entity.getRandom().nextInt(data.memory.size());
				String removed = data.memory.remove(index);
				data.adaption.remove(removed);
			}
			data.memory.add(id);
			data.adaption.put(id, 1);
		}

		int stacks = data.adaption.getOrDefault(id, 0);
		double reduction = Math.min(stacks * reductionPerStack, maxReduction);
		cache.addDealtModifier(DamageModifier.multTotal((float) (1 - reduction)));
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$addLinearInfo(List<Component> list, CallbackInfo ci) {
		if (!L2HConfig.isDisplayAdaptiveLinearEnabled()) return;

		double reductionPerStack = L2HConfig.getDisplayAdaptiveReductionPerStack();
		double maxReduction = L2HConfig.getDisplayAdaptiveMaxReduction();
		list.add(Component.translatable("trait.l2hostility_tweaks.adaptive.linear_info",
				String.format("%.0f%%", reductionPerStack * 100),
				String.format("%.0f%%", maxReduction * 100))
				.withStyle(ChatFormatting.GOLD));
	}
}
