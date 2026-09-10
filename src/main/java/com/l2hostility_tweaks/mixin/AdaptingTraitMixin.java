package com.l2hostility_tweaks.mixin;

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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

@Mixin(value = AdaptingTrait.class, remap = false)
public class AdaptingTraitMixin {

	@Group(name = "l2fix$adaptiveDamage", min = 1, max = 1)
	@Inject(method = "onDamaged(ILnet/minecraft/world/entity/LivingEntity;Ldev/xkmc/l2damagetracker/contents/attack/AttackCache;)V",
			at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 0)
	private void l2fix$adaptiveAdditiveReduction(int level, LivingEntity entity, AttackCache cache, CallbackInfo ci) {
		var event = cache.getLivingDamageEvent();
		if (event == null) return;
		Float multiplier = l2fix$getDamageMultiplier(level, entity, event.getSource());
		if (multiplier == null) return;
		ci.cancel();
		if (multiplier != 1f) cache.addDealtModifier(DamageModifier.multTotal(multiplier));
	}

	@Dynamic("L2Hostility 2.5.5 damage callback")
	@Group(name = "l2fix$adaptiveDamage", min = 1, max = 1)
	@Inject(method = "onHurtByOthers(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
			at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 0)
	private void l2fix$adaptiveLegacyReduction(int level, LivingEntity entity, LivingHurtEvent event, CallbackInfo ci) {
		Float multiplier = l2fix$getDamageMultiplier(level, entity, event.getSource());
		if (multiplier == null) return;
		ci.cancel();
		if (multiplier != 1f) event.setAmount(event.getAmount() * multiplier);
	}

	@Unique
	private Float l2fix$getDamageMultiplier(int level, LivingEntity entity, DamageSource source) {
		var attacker = ImmunityHelper.resolveLivingAttacker(source);
		var curios = attacker == null ? ImmunityHelper.CombatCurioSnapshot.EMPTY : ImmunityHelper.getCombatCurios(attacker);
		return l2fix$resolveMultiplier(curios.bypassAdaptive(), L2HConfig.isAdaptiveLinearEnabled(), () -> {
			if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_EFFECTS)) return 0;
			AdaptingTrait self = (AdaptingTrait) (Object) this;
			var cap = MobTraitCap.HOLDER.get(entity);
			var data = (AdaptingTrait.Data) cap.getOrCreateData(self.getRegistryName(), AdaptingTrait.Data::new);
			return l2fix$updateAdaptation(data, level, source.getMsgId(), L2HConfig.getAdaptiveReductionPerStack(),
					L2HConfig.getAdaptiveMaxReduction(), entity.getRandom()::nextInt);
		});
	}

	@Unique
	private static Float l2fix$resolveMultiplier(boolean bypass, boolean enabled, DoubleSupplier reduction) {
		if (bypass) return 1f;
		return enabled ? (float) (1 - reduction.getAsDouble()) : null;
	}

	@Unique
	private static double l2fix$updateAdaptation(AdaptingTrait.Data data, int level, String id,
			double reductionPerStack, double maxReduction, IntUnaryOperator randomIndex) {
		if (level <= 0 || reductionPerStack <= 0 || maxReduction <= 0) return 0;
		int maxStacks = (int) Math.ceil(maxReduction / reductionPerStack);

		if (data.memory.contains(id)) {
			int current = data.adaption.getOrDefault(id, 0);
			int next = (int) Math.min(Math.max(0L, current) + 1, maxStacks);
			data.adaption.put(id, next);
		} else {
			if (data.memory.size() >= level) {
				int index = randomIndex.applyAsInt(data.memory.size());
				String removed = data.memory.remove(index);
				data.adaption.remove(removed);
			}
			data.memory.add(id);
			data.adaption.put(id, 1);
		}

		int stacks = data.adaption.getOrDefault(id, 0);
		return Math.min(stacks * reductionPerStack, maxReduction);
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$addLinearInfo(List<Component> list, CallbackInfo ci) {
		if (!L2HConfig.isDisplayAdaptiveLinearEnabled()) return;

		double reductionPerStack = L2HConfig.getDisplayAdaptiveReductionPerStack();
		double maxReduction = L2HConfig.getDisplayAdaptiveMaxReduction();
		list.add(Component.translatable("trait.l2hostility_tweaks.adaptive.linear_info",
				String.format(Locale.ROOT, "%.0f%%", reductionPerStack * 100),
				String.format(Locale.ROOT, "%.0f%%", maxReduction * 100))
				.withStyle(ChatFormatting.GOLD));
	}
}
