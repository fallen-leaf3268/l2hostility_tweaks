package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public class SealExpiryMixin {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:seal_expiry");
	private static final int CHECK_INTERVAL = 20;

	@Inject(method = "tick", at = @At("HEAD"))
	private void l2fix$checkSealExpiry(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) return;
		if ((self.tickCount + self.getId()) % CHECK_INTERVAL != 0) return;
		var data = self.getPersistentData();
		List<String> toRemove = null;
		long gameTime = self.level().getGameTime();
		boolean hasCap = MobTraitCap.HOLDER.isProper(self);
		MobTraitCap cap = hasCap ? MobTraitCap.HOLDER.get(self) : null;
		for (String key : data.getAllKeys()) {
			if (!key.startsWith(TraitDisableHelper.SEAL_EXPIRY_PREFIX)) continue;
			String traitId = key.substring(TraitDisableHelper.SEAL_EXPIRY_PREFIX.length());

			boolean traitGone = cap == null || cap.traits.keySet().stream().noneMatch(t -> t.getID().equals(traitId));
			if (traitGone) {
				if (toRemove == null) toRemove = new ArrayList<>();
				toRemove.add(key);
				continue;
			}

			long expiry = data.getLong(key);
			if (expiry <= 0) continue;
			if (gameTime >= expiry) {
				if (toRemove == null) toRemove = new ArrayList<>();
				toRemove.add(key);
			}
		}
		if (toRemove != null) {
			for (String key : toRemove) {
				String traitId = key.substring(TraitDisableHelper.SEAL_EXPIRY_PREFIX.length());
				LOGGER.debug("SEAL_CLEANUP entity={} traitId={}", self.getName().getString(), traitId);
				data.remove(key);
				if (cap != null) {
					TraitDisableHelper.setDisabled(self, traitId, false, false);
				}
			}
		}
	}
}
