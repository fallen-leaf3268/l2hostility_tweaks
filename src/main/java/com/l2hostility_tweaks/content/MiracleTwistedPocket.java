package com.l2hostility_tweaks.content;

import com.l2hostility_tweaks.init.L2HFEnchantments;
import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class MiracleTwistedPocket extends Item implements ICurioItem {

	public MiracleTwistedPocket(Properties properties) {
		super(properties);
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		var le = slotContext.entity();
		if (le.level().isClientSide) return;
		if (!le.isAlive()) return;
		if (!(le instanceof Player player)) return;

		int abyssLevel = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.ABYSS_POCKET.get(), stack);
		int gluttonyLevel = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.GLUTTONY_POCKET.get(), stack);

		long currentTime = le.level().getGameTime();
		long lastTick = stack.getOrCreateTag().getLong("MiracleLastTick");
		int interval = Math.max(1, 40 - gluttonyLevel * 10);
		if (currentTime - lastTick < interval) return;
		stack.getOrCreateTag().putLong("MiracleLastTick", currentTime);

		int timeReduction = 40 + abyssLevel * 20;

		for (var e : CurioCompat.getItemAccess(le)) {
			ItemStack invStack = e.get();
			if (invStack.isEmpty()) continue;
			if (!(invStack.getItem() instanceof SealedItem)) continue;

			var tag = invStack.getOrCreateTag();
			var data = tag.getCompound(SealedItem.DATA);
			if (data.isEmpty()) continue;

			int sealTime = tag.getInt(SealedItem.TIME);
			int newTime = sealTime - timeReduction;

			if (newTime <= 0) {
				e.set(ItemStack.of(data));
			} else {
				tag.putInt(SealedItem.TIME, newTime);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		int gluttonyLevel = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.GLUTTONY_POCKET.get(), stack);
		int abyssLevel = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.ABYSS_POCKET.get(), stack);
		float interval = Math.max(1, 40 - gluttonyLevel * 10) / 20f;
		float reduction = (40 + abyssLevel * 20) / 20f;
		tooltip.add(Component.translatable("tooltip.l2hostility_tweaks.miracle_twisted_pocket",
				String.format(Locale.ROOT, "%.1f", interval).replaceAll("\\.0$", ""),
				String.format(Locale.ROOT, "%.1f", reduction).replaceAll("\\.0$", "")).withStyle(ChatFormatting.DARK_GREEN));
	}
}
