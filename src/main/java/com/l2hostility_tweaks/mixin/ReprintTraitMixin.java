package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HFEnchantments;

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

import java.util.List;

@Mixin(value = ReprintTrait.class, remap = false)
public class ReprintTraitMixin {

	@Unique
	private int l2fix$antiReprintTotal;
	@Unique
	private int l2fix$antiReprintArmor;
	@Unique
	private boolean l2fix$linear;
	@Unique
	private boolean l2fix$hasCounter;
	@Unique
	private static Enchantment cachedVoidTouch;

	@Inject(method = "onHurtTarget", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$head(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache, CallbackInfo ci) {
		l2fix$antiReprintTotal = 0;
		l2fix$antiReprintArmor = 0;
		l2fix$hasCounter = false;
		l2fix$linear = L2HConfig.isReprintLinearEnabled();

		Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
		for (var slot : EquipmentSlot.values()) {
			ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
			for (var e : src.getAllEnchantments().entrySet()) {
				if (e.getKey() == antiReprint) {
					l2fix$antiReprintTotal += e.getValue();
					l2fix$hasCounter = true;
					if (slot.getType() == EquipmentSlot.Type.ARMOR) {
						l2fix$antiReprintArmor = Math.max(l2fix$antiReprintArmor, e.getValue());
					}
				}
			}
		}

		if (attacker instanceof Player) {
			ci.cancel();
			l2fix$playerReprint(attacker, cache);
			return;
		}

		if (l2fix$linear || l2fix$hasCounter) {
			ci.cancel();
			l2fix$handleReprint(level, attacker, cache, traitCache);
		}
	}

	@Unique
	private void l2fix$handleReprint(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
		Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
		if (cachedVoidTouch == null) {
			cachedVoidTouch = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("l2complements", "void_touch"));
		}
		Enchantment voidTouch = cachedVoidTouch;

		long total = 0;
		int maxLv = 0;
		var event = cache.getLivingHurtEvent();
		for (var slot : EquipmentSlot.values()) {
			ItemStack dst = attacker.getItemBySlot(slot);
			ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
			var targetEnch = src.getAllEnchantments();
			for (var e : targetEnch.entrySet()) {
				int lv = e.getValue();
				boolean isCounter = e.getKey() == antiReprint;

				if (l2fix$linear) {
					if (isCounter) {
						total -= lv;
					} else {
						maxLv = Math.max(maxLv, lv);
						total += lv;
					}
				} else {
					if (!isCounter) {
						maxLv = Math.max(maxLv, lv);
					}
					if (lv >= 30 && !isCounter) {
						total = -1;
					} else if (total >= 0) {
						long contribution = 1L << (lv - 1);
						total += isCounter ? -contribution : contribution;
					}
				}
			}

			if (event != null && event.getSource().getDirectEntity() == attacker)
				ReprintHandler.reprint(dst, src);
		}

		if (total < 0 && total != -1) {
			total = 0;
		}

		int bypass = l2fix$linear ? 11 : LHConfig.COMMON.reprintBypass.get();
		if (maxLv >= bypass) {
			ItemStack weapon = attacker.getItemBySlot(EquipmentSlot.MAINHAND);
			if (!weapon.isEmpty() && (weapon.isEnchanted() || weapon.isEnchantable())) {
				if (voidTouch != null && weapon.canApplyAtEnchantingTable(voidTouch)) {
					int vtLv = l2fix$linear ? Math.min(maxLv - bypass + 1, 20) : 20;
					var map = weapon.getAllEnchantments();
					map.compute(voidTouch, (k, v) -> v == null ? vtLv : Math.max(v, vtLv));
					map.compute(Enchantments.VANISHING_CURSE, (k, v) -> v == null ? 1 : Math.max(v, 1));
					EnchantmentHelper.setEnchantments(map, weapon);
				}
			}
		}

		float factor;
		if (l2fix$linear) {
			factor = total;
		} else if (total >= 0) {
			factor = total;
		} else {
			int exponent = Math.max(0, maxLv - 1 - l2fix$antiReprintTotal);
			factor = (float) Math.pow(2, exponent);
		}

		if (l2fix$antiReprintArmor > 0) {
			float reduction = l2fix$antiReprintArmor * (float) L2HConfig.getAntiReprintReduction();
			cache.addHurtModifier(DamageModifier.multTotal(1 - Math.min(reduction, 0.8f)));
		}
		cache.addHurtModifier(DamageModifier.multTotal(1 + (float) (L2HConfig.getReprintDamage() * factor)));
	}

	@Unique
	private void l2fix$playerReprint(LivingEntity attacker, AttackCache cache) {
		long total = 0;
		int maxLv = 0;

		for (var slot : EquipmentSlot.values()) {
			ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
			for (var e : src.getAllEnchantments().entrySet()) {
				int lv = e.getValue();
				if (l2fix$linear) {
					maxLv = Math.max(maxLv, lv);
					Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
					total += e.getKey() == antiReprint ? -lv : lv;
				} else {
					Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
					boolean isCounter = e.getKey() == antiReprint;
					if (!isCounter) {
						maxLv = Math.max(maxLv, lv);
					}
					if (lv >= 30 && !isCounter) {
						total = -1;
					} else if (total >= 0) {
						long contribution = 1L << (lv - 1);
						total += isCounter ? -contribution : contribution;
					}
				}
			}
		}

		if (total < 0 && total != -1) {
			total = 0;
		}

		float factor;
		if (l2fix$linear) {
			factor = total;
		} else if (total >= 0) {
			factor = total;
		} else {
			int exponent = Math.max(0, maxLv - 1 - l2fix$antiReprintTotal);
			factor = (float) Math.pow(2, exponent);
		}

		if (l2fix$antiReprintArmor > 0) {
			float reduction = l2fix$antiReprintArmor * (float) L2HConfig.getAntiReprintReduction();
			cache.addHurtModifier(DamageModifier.multTotal(1 - Math.min(reduction, 0.8f)));
		}
		cache.addHurtModifier(DamageModifier.multTotal(1 + (float) (L2HConfig.getReprintDamage() * factor)));
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$addLinearInfo(List<Component> list, CallbackInfo ci) {
		if (!L2HConfig.isReprintLinearEnabled()) return;

		list.add(Component.translatable("trait.l2hostility_tweaks.reprint.linear_info",
				String.format("%.0f%%", L2HConfig.getReprintDamage() * 100))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
