package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2hostility.content.item.traits.EffectBooster;
import dev.xkmc.l2hostility.content.traits.highlevel.DrainTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.IntFunction;

@Mixin(value = DrainTrait.class, remap = false)
public class DrainTraitMixin {

	@Unique
	private static final TagKey<MobEffect> l2fix$drainIgnore =
			TagKey.create(Registries.MOB_EFFECT, new ResourceLocation("l2hostility", "drain_ignore"));

	@Inject(method = "onHurtTarget", at = @At("HEAD"), cancellable = true)
	private void l2fix$drainOnHurt(int level, LivingEntity attacker, AttackCache cache,
									dev.xkmc.l2hostility.content.logic.TraitEffectCache traitCache, CallbackInfo ci) {
		ci.cancel();
		((dev.xkmc.l2hostility.content.traits.base.MobTrait)(Object)this).postHurtPlayer(level, attacker, traitCache);
		var target = cache.getAttackTarget();
		var neg = target.getActiveEffects().stream()
				.filter(e -> e.getEffect().getCategory() == MobEffectCategory.HARMFUL).count();
		cache.addHurtModifier(DamageModifier.multTotal((float) (1 + L2HConfig.getDrainDamage(level) * neg)));
	}

	@Inject(method = "postHurtImpl", at = @At("HEAD"), cancellable = true)
	private void l2fix$drainPostHurt(int level, LivingEntity attacker, LivingEntity target, CallbackInfo ci) {
		ci.cancel();
		var ignored = BuiltInRegistries.MOB_EFFECT.getTag(l2fix$drainIgnore).orElse(null);
		var pos = new ArrayList<MobEffectInstance>();
		for (var effect : target.getActiveEffects()) {
			if (effect.getEffect().getCategory() != MobEffectCategory.BENEFICIAL) continue;
			if (ignored != null && ignored.contains(
					BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.getEffect()))) continue;
			pos.add(effect);
		}
		int count = Math.min(L2HConfig.getDrainCount(level), pos.size());
		for (int i = 0; i < count; i++) {
			var ins = pos.remove(target.getRandom().nextInt(pos.size()));
			target.removeEffect(ins.getEffect());
		}
		dev.xkmc.l2hostility.content.item.traits.EffectBooster.boostTrait(target,
				1 + L2HConfig.getDrainDuration(level),
				L2HConfig.getDrainDurationMax(level));
	}

	@Redirect(method = "addDetail", at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
	private boolean l2fix$drainDetail(List<Component> list, Object component) {
		int max = ((DrainTrait) (Object) this).getMaxLevel();
		return list.add(Component.translatable(((DrainTrait) (Object) this).getDescriptionId() + ".desc",
				l2fix$mapLevel(i -> Component.literal(L2HConfig.getDrainCount(i) + "").withStyle(ChatFormatting.AQUA), max),
				l2fix$mapLevel(i -> Component.literal(
						Math.round(L2HConfig.getDrainDamage(i) * 100) + "%").withStyle(ChatFormatting.AQUA), max),
				l2fix$mapLevel(i -> Component.literal(
						Math.round(L2HConfig.getDrainDuration(i) * 100) + "%").withStyle(ChatFormatting.AQUA), max),
				l2fix$mapLevel(i -> Component.literal(
						Math.round(L2HConfig.getDrainDurationMax(i) / 20f) + "").withStyle(ChatFormatting.AQUA), max))
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
