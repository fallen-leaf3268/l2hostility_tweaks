package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HFEnchantments;
import com.l2hostility_tweaks.util.ReprintDamageCalculator;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2hostility.content.item.traits.ReprintHandler;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.highlevel.ReprintTrait;
import dev.xkmc.l2hostility.init.data.LHConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

@Mixin(value = ReprintTrait.class, remap = false)
public class ReprintTraitMixin {

	@Unique
	private static Enchantment cachedVoidTouch;

	@Inject(method = "onHurtTarget", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$head(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache, CallbackInfo ci) {
		int antiReprintArmor = 0;
		boolean hasCounter = false;
		boolean linear = L2HConfig.isReprintLinearEnabled();

		Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
		for (var slot : EquipmentSlot.values()) {
			ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
			for (var e : src.getAllEnchantments().entrySet()) {
				if (e.getKey() == antiReprint) {
					hasCounter = true;
					if (slot.getType() == EquipmentSlot.Type.ARMOR) {
						antiReprintArmor = Math.max(antiReprintArmor, e.getValue());
					}
				}
			}
		}

		if (attacker instanceof Player) {
			ci.cancel();
			l2fix$playerReprint(attacker, cache, linear, antiReprintArmor);
			return;
		}

		if (linear || hasCounter) {
			ci.cancel();
			l2fix$handleReprint(attacker, cache, linear, antiReprintArmor);
		}
	}

	@Unique
	private void l2fix$handleReprint(LivingEntity attacker, AttackCache cache,
			boolean linear, int antiReprintArmor) {
		Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
		if (cachedVoidTouch == null) {
			cachedVoidTouch = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("l2complements", "void_touch"));
		}
		Enchantment voidTouch = cachedVoidTouch;

		var points = new ArrayList<ReprintDamageCalculator.Point>();
		var event = cache.getLivingHurtEvent();
		for (var slot : EquipmentSlot.values()) {
			ItemStack dst = attacker.getItemBySlot(slot);
			ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
			for (var e : src.getAllEnchantments().entrySet()) {
				points.add(new ReprintDamageCalculator.Point(e.getValue(), e.getKey() == antiReprint));
			}

			if (event != null && event.getSource().getDirectEntity() == attacker) {
				ReprintHandler.reprint(dst, src);
			}
		}

		var result = ReprintDamageCalculator.calculate(linear, points);
		int maxLv = result.maxLevel();
		float factor = result.factor();

		int bypass = linear ? 11 : LHConfig.COMMON.reprintBypass.get();
		if (maxLv >= bypass) {
			ItemStack weapon = attacker.getItemBySlot(EquipmentSlot.MAINHAND);
			if (!weapon.isEmpty() && (weapon.isEnchanted() || weapon.isEnchantable())) {
				if (voidTouch != null && weapon.canApplyAtEnchantingTable(voidTouch)) {
					int vtLv = linear ? Math.min(maxLv - bypass + 1, 20) : 20;
					var map = weapon.getAllEnchantments();
					map.compute(voidTouch, (k, v) -> v == null ? vtLv : Math.max(v, vtLv));
					map.compute(Enchantments.VANISHING_CURSE, (k, v) -> v == null ? 1 : Math.max(v, 1));
					EnchantmentHelper.setEnchantments(map, weapon);
				}
			}
		}

		if (antiReprintArmor > 0) {
			float reduction = (float) ReprintDamageCalculator.counterReduction(
					antiReprintArmor, L2HConfig.getAntiReprintReduction());
			cache.addHurtModifier(DamageModifier.multTotal(1 - reduction));
		}
		cache.addHurtModifier(DamageModifier.multTotal(1 + (float) (L2HConfig.getReprintDamage() * factor)));
	}

	@Unique
	private void l2fix$playerReprint(LivingEntity attacker, AttackCache cache,
			boolean linear, int antiReprintArmor) {
		Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
		var points = new ArrayList<ReprintDamageCalculator.Point>();

		for (var slot : EquipmentSlot.values()) {
			ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
			for (var e : src.getAllEnchantments().entrySet()) {
				points.add(new ReprintDamageCalculator.Point(e.getValue(), e.getKey() == antiReprint));
			}
		}

		float factor = ReprintDamageCalculator.calculate(linear, points).factor();
		if (antiReprintArmor > 0) {
			float reduction = (float) ReprintDamageCalculator.counterReduction(
					antiReprintArmor, L2HConfig.getAntiReprintReduction());
			cache.addHurtModifier(DamageModifier.multTotal(1 - reduction));
		}
		cache.addHurtModifier(DamageModifier.multTotal(1 + (float) (L2HConfig.getReprintDamage() * factor)));
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$addLinearInfo(List<Component> list, CallbackInfo ci) {
		if (!L2HConfig.isDisplayReprintLinearEnabled()) return;

		list.add(Component.translatable("trait.l2hostility_tweaks.reprint.linear_info",
				String.format(Locale.ROOT, "%.0f%%", L2HConfig.getDisplayReprintDamage() * 100))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
