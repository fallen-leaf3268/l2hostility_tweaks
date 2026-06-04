package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import org.objectweb.asm.Opcodes;
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

	private static LinkedHashMap<MobTrait, Integer> l2fix$filter(LinkedHashMap<MobTrait, Integer> raw) {
		for (var v : raw.values()) {
			if (v <= 0) {
				LinkedHashMap<MobTrait, Integer> f = new LinkedHashMap<>();
				for (var e : raw.entrySet()) {
					if (e.getValue() > 0) f.put(e.getKey(), e.getValue());
				}
				return f;
			}
		}
		return raw;
	}

	@Redirect(method = "traitEvent", at = @At(value = "FIELD",
			target = "Ldev/xkmc/l2hostility/content/capability/mob/MobTraitCap;traits:Ljava/util/LinkedHashMap;",
			opcode = Opcodes.GETFIELD), remap = false)
	private LinkedHashMap<MobTrait, Integer> filterEvent(MobTraitCap cap) {
		return l2fix$filter(cap.traits);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V",
			ordinal = 0), remap = false)
	private void l2fix$filterTickPostInit(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		l2fix$filter(map).forEach(cons);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V",
			ordinal = 1), remap = false)
	private void l2fix$filterTickEffect(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		l2fix$filter(map).forEach(cons);
	}
}
