package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MobTraitCap.class, remap = false)
public abstract class MobTraitCapTickMixin {
	@Unique
	private boolean l2fix$sealStateInitialized;

	@Accessor("stage")
	public abstract MobTraitCap.Stage getStage();

	@Accessor("stage")
	public abstract void setStage(MobTraitCap.Stage stage);

	@Inject(method = "tick", at = @At("HEAD"), remap = false)
	public void l2fix$skipPlayerAutoInit(LivingEntity mob, CallbackInfo ci) {
		if (mob instanceof Player && getStage() == MobTraitCap.Stage.PRE_INIT) {
			setStage(MobTraitCap.Stage.POST_INIT);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"), remap = false)
	private void l2fix$maintainSealState(LivingEntity mob, CallbackInfo ci) {
		if (mob.level().isClientSide()) return;
		if (!l2fix$sealStateInitialized) {
			l2fix$sealStateInitialized = true;
			com.l2hostility_tweaks.util.TraitDisableHelper.maintainSealState(mob);
			return;
		}
		if ((mob.tickCount + mob.getId()) % 20 != 0) return;
		if (!com.l2hostility_tweaks.util.TraitDisableHelper.hasSealStateMarker(mob.getPersistentData())) return;
		com.l2hostility_tweaks.util.TraitDisableHelper.maintainSealState(mob);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ldev/xkmc/l2hostility/content/traits/base/MobTrait;tick(Lnet/minecraft/world/entity/LivingEntity;I)V"), remap = false)
	public void l2fix$skipPlayerTraitTick(MobTrait trait, LivingEntity entity, int level) {
		if (!ImmunityHelper.isImmuneToTraitTick(entity, trait)) {
			trait.tick(entity, level);
		}
	}
}
