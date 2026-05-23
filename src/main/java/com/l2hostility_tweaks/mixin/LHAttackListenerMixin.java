package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(value = dev.xkmc.l2hostility.events.LHAttackListener.class, remap = false)
public class LHAttackListenerMixin {

	@Redirect(method = "onHurt", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;entrySet()Ljava/util/Set;"), remap = false)
	private Set<Map.Entry<MobTrait, Integer>> l2fix$filterBonusEntrySet(LinkedHashMap<MobTrait, Integer> map) {
		return map.entrySet().stream()
				.filter(e -> e.getValue() > 0)
				.collect(Collectors.toSet());
	}
}
