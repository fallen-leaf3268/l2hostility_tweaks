package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class DispealEquipmentTickMixin {

	private static final String ROOT = "l2hostility_enchantment";
	private static final String OLD = "originalEnchantments";
	private static final String TIME = "startTime";
	private static final String ENCH = "Enchantments";
	private static final int CHECK_INTERVAL = 20;

	@Inject(method = "tick", at = @At("HEAD"))
	private void l2fix$tickEquipmentDispell(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) return;
		if (self.tickCount % CHECK_INTERVAL != 0) return;

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = self.getItemBySlot(slot);
			if (stack.isEmpty() || stack.getTag() == null) continue;
			if (!stack.getTag().contains(ROOT, Tag.TAG_COMPOUND)) continue;

			CompoundTag root = stack.getTag();
			CompoundTag tag = root.getCompound(ROOT);
			if (self.level().getGameTime() < tag.getLong(TIME)) continue;

			ListTag saved = tag.getList(OLD, Tag.TAG_COMPOUND);
			ListTag current = root.getList(ENCH, Tag.TAG_COMPOUND);
			TraitDisableHelper.mergeEnchantments(saved, current);
			root.put(ENCH, saved);
			root.remove(ROOT);
		}
	}
}
