package com.l2hostility_tweaks.content;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import com.l2hostility_tweaks.util.TraitWandHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

public class TraitSeal extends Item {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:trait_seal");

	public static ItemStack set(ItemStack ans, MobTrait trait) {
		return TraitWandHelper.setTrait(ans, trait);
	}

	public static MobTrait get(ItemStack stack) {
		return TraitWandHelper.getTrait(stack);
	}

	private static MobTrait next(MobTrait mod) {
		return TraitWandHelper.nextTrait(mod);
	}

	private static MobTrait prev(MobTrait mod) {
		return TraitWandHelper.prevTrait(mod);
	}

	public TraitSeal(Properties properties) {
		super(properties);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return 1;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (player.level().isClientSide()) return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			cycleAndNotify(stack, player);
			return InteractionResult.SUCCESS;
		}

		if (!MobTraitCap.HOLDER.isProper(target)) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.SEAL_NOT_A_MOB)
					.withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}

		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		MobTrait trait = get(stack);
		if (trait == null) return InteractionResult.FAIL;
		String id = trait.getID();

		if (!cap.traits.containsKey(trait)) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.SEAL_NO_TRAIT,
					trait.getDesc()).withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}

		boolean disabled = TraitDisableHelper.isDisabled(target, id);
		String sealKey = TraitDisableHelper.sealExpiryKey(id);
		if (disabled) {
			LOGGER.info("SEAL_UNSEAL entity={} traitId={} key={}",
					target.getName().getString(), id, sealKey);
			target.getPersistentData().remove(sealKey);
			TraitDisableHelper.setDisabled(target, id, false, false);
		} else {
			int duration = L2HConfig.getTraitSealDuration();
			if (duration > 0) {
				long expiry = target.level().getGameTime() + duration * 20L;
				LOGGER.info("SEAL_TIMED entity={} traitId={} key={} duration={} expiry={} gameTime={}",
						target.getName().getString(), id, sealKey, duration, expiry, target.level().getGameTime());
				target.getPersistentData().putLong(sealKey, expiry);
			} else {
				LOGGER.info("SEAL_PERMANENT entity={} traitId={} key={}",
						target.getName().getString(), id, sealKey);
				target.getPersistentData().putLong(sealKey, -1);
			}
			TraitDisableHelper.setDisabled(target, id, true, false);
		}

		String key = disabled ? L2HTweaksLang.SEAL_UNSEALED : L2HTweaksLang.SEAL_SEALED;
		player.displayClientMessage(L2HTweaksLang.translate(key, trait.getDesc())
				.withStyle(disabled ? ChatFormatting.GREEN : ChatFormatting.GOLD), true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			cycleAndNotify(stack, player);
		}
		return InteractionResultHolder.success(stack);
	}

	private void cycleAndNotify(ItemStack stack, Player player) {
		MobTrait old = get(stack);
		if (old == null) return;
		MobTrait nextTrait = player.isShiftKeyDown() ? prev(old) : next(old);
		set(stack, nextTrait);
		player.sendSystemMessage(L2HTweaksLang.translate(L2HTweaksLang.SEAL_SELECTED,
				nextTrait.getDesc().withStyle(ChatFormatting.AQUA)));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
		list.add(L2HTweaksLang.translate(L2HTweaksLang.SEAL_TOOLTIP).withStyle(ChatFormatting.GRAY));
		MobTrait trait = get(stack);
		if (trait != null)
			list.add(L2HTweaksLang.translate(L2HTweaksLang.SEAL_CURRENT,
					trait.getDesc().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
	}
}
