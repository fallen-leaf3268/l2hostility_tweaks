package com.l2hostility_tweaks.content;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import com.l2hostility_tweaks.util.TraitWandHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TraitUnloaderWand extends Item {

	private static final String TAG_MODE = "l2hUnloaderMode";
	private static final int MODE_SINGLE = 0;
	private static final int MODE_GROUP = 1;
	private static final int MODE_FULL = 2;

	public static ItemStack set(ItemStack ans, MobTrait trait) {
		return TraitWandHelper.setTrait(ans, trait);
	}

	public static MobTrait get(ItemStack stack) {
		return TraitWandHelper.getTrait(stack);
	}

	public static MobTrait next(MobTrait mod) {
		return TraitWandHelper.nextTrait(mod);
	}

	public static MobTrait prev(MobTrait mod) {
		return TraitWandHelper.prevTrait(mod);
	}

	public TraitUnloaderWand(Properties properties) {
		super(properties);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return 1;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (!level.isClientSide()) {
				cycleMode(stack, player);
			}
			return InteractionResultHolder.success(stack);
		}
		if (!level.isClientSide()) {
			unloadTraits(player, stack);
		}
		return InteractionResultHolder.success(stack);
	}

	private void cycleMode(ItemStack stack, Player player) {
		int mode = getMode(stack);
		mode = (mode + 1) % 3;
		setMode(stack, mode);
		player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_MODE_CHANGED,
				getModeDisplay(mode)).withStyle(ChatFormatting.GREEN), true);
	}

	private void unloadTraits(Player player, ItemStack stack) {
		if (!MobTraitCap.HOLDER.isProper(player)) return;
		MobTraitCap cap = MobTraitCap.HOLDER.get(player);
		if (!cap.isInitialized() || cap.traits.isEmpty()) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_NO_TRAITS)
					.withStyle(ChatFormatting.RED), true);
			return;
		}

		int mode = getMode(stack);
		switch (mode) {
			case MODE_SINGLE -> unloadSingle(player, cap, stack);
			case MODE_GROUP -> unloadGroup(player, cap, stack);
			case MODE_FULL -> unloadFull(player, cap);
		}
	}

	private void sync(Player player, MobTraitCap cap) {
		cap.syncToClient(player);
		if (player instanceof ServerPlayer sp) {
			cap.syncToPlayer(player, sp);
		}
	}

	private void unloadSingle(Player player, MobTraitCap cap, ItemStack stack) {
		MobTrait trait = get(stack);
		if (trait == null) return;
		Integer currentLevel = cap.traits.get(trait);
		if (currentLevel == null || currentLevel == 0) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_NO_SELECTED_TRAIT,
					trait.getDesc()).withStyle(ChatFormatting.RED), true);
			return;
		}

		int absLevel = Math.abs(currentLevel);
		float hpRatio = player.getHealth() / player.getMaxHealth();
		int newLevel = absLevel - 1;
		cap.traits.put(trait, Math.max(0, newLevel));
		trait.initialize(player, Math.max(0, newLevel));
		trait.postInit(player, Math.max(0, newLevel));
		if (newLevel <= 0) {
			cap.traits.remove(trait);
		}
		player.getPersistentData().remove(TraitDisableHelper.sealExpiryKey(trait.getID()));
		sync(player, cap);
		player.setHealth(Math.max(1, player.getMaxHealth() * Math.min(1, hpRatio)));

		int refund = L2HConfig.getUnloadRefund(absLevel);
		ItemStack symbol = new ItemStack(trait.asItem(), refund);
		player.addItem(symbol);
		player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_SINGLE,
				trait.getDesc(), absLevel, Math.max(0, newLevel)).withStyle(ChatFormatting.GREEN), true);
	}

	private void unloadGroup(Player player, MobTraitCap cap, ItemStack stack) {
		MobTrait trait = get(stack);
		if (trait == null) return;
		Integer currentLevel = cap.traits.get(trait);
		if (currentLevel == null || currentLevel == 0) {
			player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_NO_SELECTED_TRAIT,
					trait.getDesc()).withStyle(ChatFormatting.RED), true);
			return;
		}
		int absLevel = Math.abs(currentLevel);
		float hpRatio = player.getHealth() / player.getMaxHealth();
		cap.traits.put(trait, 0);
		trait.initialize(player, 0);
		trait.postInit(player, 0);
		cap.traits.remove(trait);
		player.getPersistentData().remove(TraitDisableHelper.sealExpiryKey(trait.getID()));
		sync(player, cap);
		player.setHealth(Math.max(1, player.getMaxHealth() * Math.min(1, hpRatio)));

		int totalRefund = L2HConfig.getTotalUnloadRefund(absLevel);
		ItemStack symbol = new ItemStack(trait.asItem(), totalRefund);
		player.addItem(symbol);
		player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_GROUP,
				trait.getDesc(), absLevel, totalRefund).withStyle(ChatFormatting.GREEN), true);
	}

	private void unloadFull(Player player, MobTraitCap cap) {
		List<Map.Entry<MobTrait, Integer>> entries = new ArrayList<>(cap.traits.entrySet());
	float hpRatio = player.getHealth() / player.getMaxHealth();
		int total = 0;
		for (var entry : entries) {
			MobTrait trait = entry.getKey();
			int absCount = Math.abs(entry.getValue());
			int refund = L2HConfig.getTotalUnloadRefund(absCount);
			total += refund;
			cap.traits.put(trait, 0);
			trait.initialize(player, 0);
			trait.postInit(player, 0);
			cap.traits.remove(trait);
			player.getPersistentData().remove(TraitDisableHelper.sealExpiryKey(trait.getID()));
			ItemStack symbol = new ItemStack(trait.asItem(), refund);
			player.addItem(symbol);
		}
		sync(player, cap);
		player.setHealth(Math.max(1, player.getMaxHealth() * Math.min(1, hpRatio)));
		player.displayClientMessage(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_FULL,
				entries.size(), total).withStyle(ChatFormatting.GREEN), true);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
		list.add(L2HTweaksLang.translate(L2HTweaksLang.UNLOADER_TOOLTIP).withStyle(ChatFormatting.GRAY));
		int mode = getMode(stack);
		list.add(Component.translatable(L2HTweaksLang.UNLOADER_MODE, getModeDisplay(mode))
				.withStyle(ChatFormatting.AQUA));
		MobTrait trait = get(stack);
		list.add(L2HTweaksLang.translate(L2HTweaksLang.SEAL_CURRENT,
				trait.getDesc().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
	}

	private static Component getModeDisplay(int mode) {
		return switch (mode) {
			case MODE_SINGLE -> Component.translatable(L2HTweaksLang.UNLOADER_MODE_SINGLE);
			case MODE_GROUP -> Component.translatable(L2HTweaksLang.UNLOADER_MODE_GROUP);
			case MODE_FULL -> Component.translatable(L2HTweaksLang.UNLOADER_MODE_FULL);
			default -> Component.literal("???");
		};
	}

	private int getMode(ItemStack stack) {
		CompoundTag tag = stack.getOrCreateTag();
		if (tag.contains(TAG_MODE)) {
			return tag.getInt(TAG_MODE);
		}
		return MODE_SINGLE;
	}

	private void setMode(ItemStack stack, int mode) {
		stack.getOrCreateTag().putInt(TAG_MODE, mode);
	}
}
