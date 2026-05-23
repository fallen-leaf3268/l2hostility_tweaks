package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.network.NetworkHandler;
import dev.xkmc.l2hostility.content.item.tool.DetectorGlasses;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

	private static final String TAG_GLOW_DISABLED = "DetectorGlowDisabled";

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void l2fix$toggleGlowOnRightClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (button != 1) return;

		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		Slot slot = screen.getSlotUnderMouse();
		if (slot == null || !slot.hasItem()) return;

		ItemStack stack = slot.getItem();
		if (stack.getItem() instanceof DetectorGlasses) {
			NetworkHandler.sendToggleToServer();
			cir.setReturnValue(true);
			return;
		}
		if (stack.getItem() instanceof DimensionBreakerItem) {
			NetworkHandler.sendToggleProtectToServer();
			cir.setReturnValue(true);
		}
	}
}
