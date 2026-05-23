package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.logic.TraitManager;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MobTrait.class, remap = false)
public class RealityTraitMixin {

	private static final ResourceLocation REALITY_TRAIT_ID =
			new ResourceLocation("curseofpandora", "reality");
	private static final ResourceLocation REALITY_ATTR_ID =
			new ResourceLocation("curseofpandora", "reality_index");

	@Inject(method = "initialize", at = @At("TAIL"), remap = false)
	private void l2fix$realityTraitInit(LivingEntity le, int level, CallbackInfo ci) {
		if (!ModList.get().isLoaded("curseofpandora")) return;
		if (le instanceof Player) return;
		if (!REALITY_TRAIT_ID.equals(((MobTrait) (Object) this).getRegistryName())) return;
		Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(REALITY_ATTR_ID);
		if (attr == null) return;
		TraitManager.addAttribute(le, attr, "l2hostility_tweaks:reality_suppression",
				level, AttributeModifier.Operation.ADDITION);
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$realityTraitDetail(List<Component> list, CallbackInfo ci) {
		if (TraitDisableHelper.isHideRealityDetail()) return;
		if (!ModList.get().isLoaded("curseofpandora")) return;
		if (!REALITY_TRAIT_ID.equals(((MobTrait) (Object) this).getRegistryName())) return;
		list.add(Component.translatable("trait.l2hostility_tweaks.reality.modified",
				Component.literal("1").withStyle(ChatFormatting.AQUA)));
	}
}
