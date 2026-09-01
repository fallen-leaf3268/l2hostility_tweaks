package com.l2hostility_tweaks.content;

import com.l2hostility_tweaks.util.EntityImmunityCache;
import com.l2hostility_tweaks.util.ImmunityHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.List;

public class DimensionBreakerItem extends Item {

	private static final String TAG_PROTECT = "DimensionBreakerProtect";

	public DimensionBreakerItem(Properties properties) {
		super(properties);
	}

	public static boolean isEquippedBy(LivingEntity entity) {
		return getEquippedState(entity).equipped();
	}

	public static EntityImmunityCache.DimensionBreakerState getEquippedState(LivingEntity entity) {
		return ImmunityHelper.getDimensionBreakerState(entity);
	}

	public static EntityImmunityCache.DimensionBreakerState computeEquippedState(LivingEntity entity) {
		boolean equipped = false;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.getItem() instanceof DimensionBreakerItem) {
				equipped = true;
				if (isProtectMode(stack)) return EntityImmunityCache.DimensionBreakerState.PROTECTED;
			}
		}
		try {
			var inventory = CuriosApi.getCuriosInventory(entity).resolve();
			if (inventory.isPresent()) {
				for (var stacksHandler : inventory.get().getCurios().values()) {
					var stacks = stacksHandler.getStacks();
					for (int i = 0; i < stacks.getSlots(); i++) {
						ItemStack stack = stacks.getStackInSlot(i);
						if (stack.getItem() instanceof DimensionBreakerItem) {
							equipped = true;
							if (isProtectMode(stack)) {
								return EntityImmunityCache.DimensionBreakerState.PROTECTED;
							}
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		return equipped ? EntityImmunityCache.DimensionBreakerState.EQUIPPED
				: EntityImmunityCache.DimensionBreakerState.EMPTY;
	}

	@Nullable
	public static ItemStack findEquipped(LivingEntity entity) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.getItem() instanceof DimensionBreakerItem) {
				return stack;
			}
		}
		try {
			return CuriosApi.getCuriosInventory(entity).resolve().map(handler -> {
				for (var stacksHandler : handler.getCurios().values()) {
					var stacks = stacksHandler.getStacks();
					for (int i = 0; i < stacks.getSlots(); i++) {
						ItemStack stack = stacks.getStackInSlot(i);
						if (stack.getItem() instanceof DimensionBreakerItem) {
							return stack;
						}
					}
				}
				return ItemStack.EMPTY;
			}).orElse(ItemStack.EMPTY);
		} catch (Exception ignored) {
			return ItemStack.EMPTY;
		}
	}

	public static boolean isProtectMode(ItemStack stack) {
		return stack.hasTag() && stack.getOrCreateTag().getBoolean(TAG_PROTECT);
	}

	public static boolean isProtectActive(Player player) {
		return getEquippedState(player).protectActive();
	}

	public static void toggleProtect(ItemStack stack) {
		boolean current = isProtectMode(stack);
		stack.getOrCreateTag().putBoolean(TAG_PROTECT, !current);
	}

	private static boolean isCorrectBlockType(BlockState state) {
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
			|| state.is(BlockTags.MINEABLE_WITH_AXE)
			|| state.is(BlockTags.MINEABLE_WITH_SHOVEL);
	}

	public static boolean canHarvest(Player player, BlockState state) {
		return isEquippedBy(player) && isCorrectBlockType(state);
	}

	public static float getSpeed(Player player, BlockState state) {
		if (isEquippedBy(player) && isCorrectBlockType(state)) {
			return Tiers.NETHERITE.getSpeed();
		}
		return -1;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
		list.add(Component.translatable("tooltip.l2hostility_tweaks.dimension_breaker").withStyle(ChatFormatting.GOLD));
		boolean protect = isProtectMode(stack);
		list.add(Component.translatable(protect
				? "tooltip.l2hostility_tweaks.dimension_breaker.protect_on"
				: "tooltip.l2hostility_tweaks.dimension_breaker.protect_off")
				.withStyle(protect ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}
}
