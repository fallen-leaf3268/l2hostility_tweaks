package com.l2hostilityfix.client;

import com.l2hostilityfix.config.L2HConfig;
import com.l2hostilityfix.init.L2HFEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = "l2hostilityfix")
public class ClientEventHandler {

	@SubscribeEvent
	public static void registerGuiOverlayEvent(RegisterGuiOverlaysEvent evt) {
		evt.registerBelow(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(),
				"l2hostilityfix_health_overlay", new L2HHealthOverlay());
	}

	@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "l2hostilityfix")
	public static class ForgeEvents {

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void onItemTooltip(ItemTooltipEvent event) {
			ItemStack stack = event.getItemStack();
			int lv = EnchantmentHelper.getEnchantments(stack).getOrDefault(L2HFEnchantments.REPRINT_COUNTER.get(), 0);
			if (lv == 0) return;

			String descKey = "enchantment.l2hostilityfix.reprint_counter.desc";
			String descAnyKey = "enchantment.l2hostilityfix.reprint_counter.desc_any";
			for (int i = 0; i < event.getToolTip().size(); i++) {
				var comp = event.getToolTip().get(i);
				if (comp.getContents() instanceof TranslatableContents tr && tr.getKey().equals(descKey)) {
					boolean isArmor = false;
					for (EquipmentSlot es : EquipmentSlot.values()) {
						if (es.getType() == EquipmentSlot.Type.ARMOR && !stack.getAttributeModifiers(es).isEmpty()) {
							isArmor = true;
							break;
						}
					}
					Component tip;
					if (isArmor || stack.is(Items.ENCHANTED_BOOK)) {
						double reduction = L2HConfig.getAntiReprintReduction() * lv;
						if (stack.is(Items.ENCHANTED_BOOK)) {
							reduction = Math.min(reduction, 0.8);
						}
						tip = Component.translatable(descKey, String.format("%.0f", reduction * 100))
								.withStyle(ChatFormatting.GRAY);
					} else {
						tip = Component.translatable(descAnyKey).withStyle(ChatFormatting.GRAY);
					}
					event.getToolTip().set(i, tip);
					return;
				}
			}
		}
	}
}
