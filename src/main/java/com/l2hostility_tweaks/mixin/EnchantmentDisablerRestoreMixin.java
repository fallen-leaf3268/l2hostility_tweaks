package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.item.traits.EnchantmentDisabler;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(value = EnchantmentDisabler.class, remap = false)
public class EnchantmentDisablerRestoreMixin {

	@Redirect(method = "tickStack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/nbt/ListTag;addAll(Ljava/util/Collection;)Z"), remap = false)
	private static boolean l2fix$mergeRestoredEnchantments(ListTag saved,
			Collection<? extends Tag> current) {
		return TraitDisableHelper.mergeEnchantments(saved, current);
	}
}
