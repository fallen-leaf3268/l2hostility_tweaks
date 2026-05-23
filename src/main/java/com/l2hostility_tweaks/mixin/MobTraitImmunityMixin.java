package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2damagetracker.init.data.L2DamageTypes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.DementorTrait;
import dev.xkmc.l2hostility.content.traits.legendary.DispellTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MobTrait.class, remap = false)
public class MobTraitImmunityMixin {

	@Inject(method = "onAttackedByOthers", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$legendaryImmunity(int level, LivingEntity entity, LivingAttackEvent event, CallbackInfo ci) {
		var source = event.getSource();
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ||
				source.is(DamageTypeTags.BYPASSES_EFFECTS) ||
				source.is(L2HFBypassTags.BYPASSES_DISPELL) ||
				source.is(L2HFBypassTags.BYPASSES_DEMENTOR))
			return;

		String id = ((MobTrait) (Object) this).getID();

		if ("l2hostility:dispell".equals(id) && L2HConfig.isOldDispellEnabled()) {
			if (source.is(L2DamageTypes.MAGIC)) event.setCanceled(true);
		}
		if ("l2hostility:dementor".equals(id) && L2HConfig.isOldDementorEnabled()) {
			if (!source.is(L2DamageTypes.MAGIC)) event.setCanceled(true);
		}
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$immunityDetail(List<Component> list, CallbackInfo ci) {
		String id = ((MobTrait) (Object) this).getID();
		if ("l2hostility:dementor".equals(id) && L2HConfig.isOldDementorEnabled()) {
			list.add(Component.translatable("trait.l2hostility_tweaks.dementor.immunity")
					.withStyle(ChatFormatting.GOLD));
		}
	}
}
