package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;

@Mixin(value = MobTraitCap.class, remap = false)
public abstract class MobTraitCapTickMixin {

	private static final Logger LOG = LogUtils.getLogger();

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

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;clear()V"), remap = false)
	public void l2fix$skipPetClear(LinkedHashMap<?, ?> instance) {
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ldev/xkmc/l2hostility/content/traits/base/MobTrait;tick(Lnet/minecraft/world/entity/LivingEntity;I)V"), remap = false)
	public void l2fix$skipPlayerTraitTick(MobTrait trait, LivingEntity entity, int level) {
		if (ImmunityHelper.isImmuneToTraitTick(entity, trait)) {
			LOG.debug("[TraitTick] BLOCKED {} for {}", trait.getID(), entity.getName().getString());
		} else {
			trait.tick(entity, level);
		}
	}
}
