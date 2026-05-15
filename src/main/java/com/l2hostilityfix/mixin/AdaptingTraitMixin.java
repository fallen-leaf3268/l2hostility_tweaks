package com.l2hostilityfix.mixin;

import com.l2hostilityfix.L2HFBypassTags;
import com.l2hostilityfix.config.L2HConfig;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.common.AdaptingTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AdaptingTrait.class, remap = false)
public class AdaptingTraitMixin {

	@Inject(method = "onHurtByOthers", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$adaptiveAdditiveReduction(int level, LivingEntity entity, LivingHurtEvent event, CallbackInfo ci) {
		if (!L2HConfig.isAdaptiveLinearEnabled()) return;

		ci.cancel();

		DamageSource source = event.getSource();
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_EFFECTS) || source.is(L2HFBypassTags.BYPASSES_ADAPTIVE)) return;

		AdaptingTrait self = (AdaptingTrait) (Object) this;
		var cap = MobTraitCap.HOLDER.get(entity);
		var data = (AdaptingTrait.Data) cap.getOrCreateData(self.getRegistryName(), () -> createData(self));

		String id = source.getMsgId();
		double reductionPerStack = L2HConfig.getAdaptiveReductionPerStack();
		double maxReduction = L2HConfig.getAdaptiveMaxReduction();
		int maxStacks = (int) Math.ceil(maxReduction / reductionPerStack);

		if (data.memory.contains(id)) {
			int current = data.adaption.getOrDefault(id, 0);
			int next = Math.min(current + 1, maxStacks);
			data.adaption.put(id, next);
		} else {
			if (data.memory.size() >= level) {
				int index = entity.getRandom().nextInt(data.memory.size());
				String removed = data.memory.remove(index);
				data.adaption.remove(removed);
			}
			data.memory.add(id);
			data.adaption.put(id, 1);
		}

		int stacks = data.adaption.getOrDefault(id, 0);
		double reduction = Math.min(stacks * reductionPerStack, maxReduction);
		event.setAmount(event.getAmount() * (float) (1 - reduction));
	}

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$addLinearInfo(List<Component> list, CallbackInfo ci) {
		if (!L2HConfig.isAdaptiveLinearEnabled()) return;

		double reductionPerStack = L2HConfig.getAdaptiveReductionPerStack();
		double maxReduction = L2HConfig.getAdaptiveMaxReduction();
		list.add(Component.translatable("trait.l2hostilityfix.adaptive.linear_info.prefix")
				.append(Component.literal(String.format("%.0f%%", reductionPerStack * 100))
						.withStyle(ChatFormatting.AQUA))
				.append(Component.translatable("trait.l2hostilityfix.adaptive.linear_info.mid"))
				.append(Component.literal(String.format("%.0f%%", maxReduction * 100))
						.withStyle(ChatFormatting.AQUA))
				.append(Component.translatable("trait.l2hostilityfix.adaptive.linear_info.suffix"))
				.withStyle(ChatFormatting.GRAY));
	}

	@Unique
	private static AdaptingTrait.Data createData(AdaptingTrait trait) {
		try {
			return AdaptingTrait.Data.class.getDeclaredConstructor(AdaptingTrait.class).newInstance(trait);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create AdaptingTrait.Data", e);
		}
	}
}
