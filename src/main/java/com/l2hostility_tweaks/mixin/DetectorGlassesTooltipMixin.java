package com.l2hostility_tweaks.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ItemStack.class)
public class DetectorGlassesTooltipMixin {

	@Unique
	private static final ResourceLocation DETECTOR_GLASSES_ID = new ResourceLocation("l2hostility", "detector_glasses");

	@Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
	public void l2fix$addDetectorGlassesTooltip(@Nullable Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir, List<Component> list) {
		ItemStack self = (ItemStack) (Object) this;
		if (DETECTOR_GLASSES_ID.equals(ForgeRegistries.ITEMS.getKey(self.getItem()))) {
			boolean disabled = self.hasTag() && self.getTag().getBoolean("DetectorGlowDisabled");
			list.add(Component.translatable(disabled ? "tooltip.l2hostility_tweaks.glow_disabled" : "tooltip.l2hostility_tweaks.glow_enabled")
					.withStyle(disabled ? ChatFormatting.RED : ChatFormatting.GREEN));
		}
	}
}
