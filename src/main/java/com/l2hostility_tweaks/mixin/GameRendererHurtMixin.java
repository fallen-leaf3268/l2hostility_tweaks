package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.ImmunityHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererHurtMixin {

	@Inject(
			method = "bobHurt",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/world/entity/LivingEntity;hurtDuration:I"),
			cancellable = true)
	private void l2fix$cancelHurtCameraTilt(PoseStack poseStack, float partialTick,
			CallbackInfo ci) {
		if (Minecraft.getInstance().getCameraEntity() instanceof LivingEntity cameraEntity
				&& ImmunityHelper.isImmuneToForce(cameraEntity)) {
			ci.cancel();
		}
	}
}
