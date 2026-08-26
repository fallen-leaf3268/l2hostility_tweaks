package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

/**
 * 封印词条（v <= 0）不参与 tick/postInit/traitEvent。
 */
@Mixin(value = MobTraitCap.class, remap = false)
public class TraitSealFilterMixin {

	static <T> void l2fix$forEachActive(LinkedHashMap<T, Integer> map, BiConsumer<T, Integer> consumer) {
		for (var entry : map.entrySet()) {
			Integer level = entry.getValue();
			if (level != null && level > 0) {
				consumer.accept(entry.getKey(), level);
			}
		}
	}

	@Redirect(method = "traitEvent", at = @At(value = "INVOKE",
			target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V"), remap = false)
	private void filterEvent(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> consumer) {
		l2fix$forEachActive(map, consumer);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V",
			ordinal = 0), remap = false)
	private void l2fix$filterTickPostInit(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		l2fix$forEachActive(map, cons);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V",
			ordinal = 1), remap = false)
	private void l2fix$filterTickEffect(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		l2fix$forEachActive(map, cons);
	}
}
