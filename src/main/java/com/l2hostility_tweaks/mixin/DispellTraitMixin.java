package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.item.traits.EnchantmentDisabler;
import dev.xkmc.l2hostility.content.traits.legendary.DispellTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

@Mixin(value = DispellTrait.class, remap = false)
public class DispellTraitMixin {

	@Inject(method = "postHurtImpl", at = @At("HEAD"), cancellable = true)
	private void l2fix$dispellPostHurt(int level, LivingEntity attacker, LivingEntity target, CallbackInfo ci) {
		ci.cancel();
		List<ItemStack> list = new ArrayList<>();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = target.getItemBySlot(slot);
			if (stack.isEnchanted() && !stack.getOrCreateTag().contains("l2hostility_enchantment")) {
				list.add(stack);
			}
		}
		if (list.isEmpty()) return;
		int time = L2HConfig.getDispellTime(level);
		int count = Math.min(L2HConfig.getDispellCount(level), list.size());
		for (int i = 0; i < count; i++) {
			int index = attacker.getRandom().nextInt(list.size());
			EnchantmentDisabler.disableEnchantment(attacker.level(), list.remove(index), time);
		}
	}

	@Redirect(method = "addDetail", at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
	private boolean l2fix$dispellDetail(List<Component> list, Object component) {
		int max = ((DispellTrait) (Object) this).getMaxLevel();
		return list.add(Component.translatable(((DispellTrait) (Object) this).getDescriptionId() + ".desc",
				l2fix$mapLevel(i -> Component.literal(L2HConfig.getDispellCount(i) + "")
						.withStyle(ChatFormatting.AQUA), max),
				l2fix$mapLevel(i -> Component.literal(
						Math.round(L2HConfig.getDispellTime(i) / 20f) + "").withStyle(ChatFormatting.AQUA), max))
				.withStyle(ChatFormatting.GRAY));
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$dispellImmunityDetail(List<Component> list, CallbackInfo ci) {
		if (L2HConfig.isOldDispellEnabled()) {
			list.add(Component.translatable("trait.l2hostility_tweaks.dispell.immunity")
					.withStyle(ChatFormatting.GOLD));
		}
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
