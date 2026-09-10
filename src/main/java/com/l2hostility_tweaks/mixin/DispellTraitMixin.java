package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2hostility.content.item.traits.EnchantmentDisabler;
import dev.xkmc.l2hostility.content.traits.legendary.DispellTrait;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

@Mixin(value = DispellTrait.class, remap = false)
public class DispellTraitMixin {

	@Group(name = "l2fix$dispellDefense", min = 1, max = 1)
	@Inject(method = "onAttackedByOthers(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraftforge/event/entity/living/LivingAttackEvent;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 0)
	private void l2fix$bypassDispellDefense(int level, LivingEntity entity, LivingAttackEvent event, CallbackInfo ci) {
		var attacker = ImmunityHelper.resolveLivingAttacker(event.getSource());
		if (attacker != null && ImmunityHelper.hasCombatCurioWithTag(attacker, L2HFBypassTags.BYPASSES_DISPELL_ITEM)) {
			ci.cancel();
			return;
		}
	}

	@Group(name = "l2fix$dispellDefense", min = 1, max = 1)
	@Inject(method = "onDamaged(ILnet/minecraft/world/entity/LivingEntity;Ldev/xkmc/l2damagetracker/contents/attack/AttackCache;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 0)
	private void l2fix$dispellDefense(int level, LivingEntity entity, AttackCache cache, CallbackInfo ci) {
		var event = cache.getLivingDamageEvent();
		if (event == null) return;
		var attacker = ImmunityHelper.resolveLivingAttacker(event.getSource());
		if (attacker != null && ImmunityHelper.hasCombatCurioWithTag(attacker, L2HFBypassTags.BYPASSES_DISPELL_ITEM)) ci.cancel();
	}

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
				l2fix$mapLevel(i -> Component.literal(L2HConfig.getDisplayDispellCount(i) + "")
						.withStyle(ChatFormatting.AQUA), max),
				l2fix$mapLevel(i -> Component.literal(
						Math.round(L2HConfig.getDisplayDispellTime(i) / 20f) + "").withStyle(ChatFormatting.AQUA), max))
				.withStyle(ChatFormatting.GRAY));
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$dispellImmunityDetail(List<Component> list, CallbackInfo ci) {
		if (L2HConfig.isDisplayOldDispellEnabled()) {
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
