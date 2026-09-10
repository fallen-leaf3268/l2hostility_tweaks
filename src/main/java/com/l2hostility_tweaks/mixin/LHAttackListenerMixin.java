package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.events.LHAttackListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.damagesource.DamageSource;
import java.util.function.DoubleSupplier;

/**
 * 封印词条（value < 0）不参与 modifyBonusDamage 伤害加成。
 * 非正等级直接返回 1，避免调用词条伤害逻辑。
 */
@Mixin(value = LHAttackListener.class, remap = false)
public class LHAttackListenerMixin {

	@Redirect(
			method = "onHurt",
			at = @At(value = "INVOKE",
					target = "Ldev/xkmc/l2hostility/content/traits/base/MobTrait;modifyBonusDamage(Lnet/minecraft/world/damagesource/DamageSource;DI)D"))
	private double l2fix$skipInactiveBonus(MobTrait trait, DamageSource source, double factor, int level) {
		return l2fix$resolveBonus(level, () -> trait.modifyBonusDamage(source, factor, level));
	}

	private static double l2fix$resolveBonus(int level, DoubleSupplier modifier) {
		return level <= 0 ? 1D : modifier.getAsDouble();
	}
}
