package com.l2hostility_tweaks.content.traits;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2library.init.events.GeneralEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class SealTrait extends LegendaryTrait {

	public SealTrait(ChatFormatting format) {
		super(format);
	}

	@Override
	public void postHurtImpl(int level, LivingEntity attacker, LivingEntity target) {
		GeneralEventHandler.schedule(() -> sealRandomTrait(level, target));
	}

	private void sealRandomTrait(int level, LivingEntity target) {
		if (!MobTraitCap.HOLDER.isProper(target)) return;
		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		if (cap.traits.isEmpty()) return;

		List<MobTrait> available = new ArrayList<>();
		for (var entry : cap.traits.entrySet()) {
			if (!TraitDisableHelper.isDisabled(target, entry.getKey().getID())) {
				available.add(entry.getKey());
			}
		}
		if (available.isEmpty()) return;

		MobTrait trait = available.get(target.getRandom().nextInt(available.size()));
		String traitId = trait.getID();
		int duration = getSealDurationTicks(level);
		long expiry = target.level().getGameTime() + duration;
		String key = TraitDisableHelper.sealExpiryKey(traitId);
		target.getPersistentData().putLong(key, expiry);
		TraitDisableHelper.setDisabled(target, traitId, true);
	}

	@Override
	public void addDetail(List<Component> list) {
		list.add(Component.translatable(getDescriptionId() + ".desc",
						mapLevel(i -> Component.literal(getSealDurationSeconds(i) + "")
								.withStyle(ChatFormatting.AQUA)))
				.withStyle(ChatFormatting.GRAY));
	}

	private int getSealDurationSeconds(int level) {
		int mode = L2HConfig.getSealDurationMode();
		int linear = L2HConfig.getSealDurationLinear();
		if (mode == 2) {
			List<Integer> array = L2HConfig.getSealDurationArray();
			if (array.isEmpty()) return level * linear;
			if (level <= array.size()) {
				return array.get(level - 1);
			}
			int lastValue = array.get(array.size() - 1);
			return lastValue + (level - array.size()) * linear;
		}
		return level * linear;
	}

	private int getSealDurationTicks(int level) {
		return getSealDurationSeconds(level) * 20;
	}
}
