package com.l2hostility_tweaks.content;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TranquilBeltItem extends Item implements ICurioItem {

	public TranquilBeltItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.l2hostility_tweaks.tranquil_belt").withStyle(ChatFormatting.GOLD));
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
		Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
		map.put(Attributes.KNOCKBACK_RESISTANCE,
				new AttributeModifier(uuid, "tranquil_belt_knockback", 1.0,
						AttributeModifier.Operation.ADDITION));
		return map;
	}

	private static LivingEntity cachedEntity;
	private static int cacheTick = -1;
	private static boolean cachedWearing;

	public static boolean isWearing(LivingEntity entity) {
		if (entity.tickCount == cacheTick && entity == cachedEntity) {
			return cachedWearing;
		}
		boolean result = CuriosApi.getCuriosInventory(entity).resolve().map(handler ->
				handler.findFirstCurio(stack -> stack.getItem() instanceof TranquilBeltItem).isPresent()
		).orElse(false);
		cacheTick = entity.tickCount;
		cachedEntity = entity;
		cachedWearing = result;
		return result;
	}
}
