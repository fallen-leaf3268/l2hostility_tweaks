package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import net.minecraft.world.entity.LivingEntity;
import com.l2hostility_tweaks.util.TraitDisableHelper;
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

	@Inject(method = "tick", at = @At("HEAD"))
	private void l2fix$checkSealExpiry(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) return;
		var data = self.getPersistentData();
		List<String> toRemove = null;
		long gameTime = self.level().getGameTime();
		for (String key : data.getAllKeys()) {
			if (!key.startsWith(TraitDisableHelper.SEAL_EXPIRY_PREFIX)) continue;
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
				LOGGER.debug("SEAL_EXPIRED entity={} traitId={}", self.getName().getString(), traitId);
				data.remove(key);
				TraitDisableHelper.setDisabled(self, traitId, false, false);
			}
		}
	}
}
