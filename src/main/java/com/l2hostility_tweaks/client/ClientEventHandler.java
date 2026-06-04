package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.content.TraitUnloaderWand;
import com.l2hostility_tweaks.init.L2HFEnchantments;
import com.l2hostility_tweaks.network.NetworkHandler;
import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.data.LangData;
import dev.xkmc.l2hostility.init.registrate.LHItems;
import dev.xkmc.l2tabs.tabs.core.TabRegistry;
import dev.xkmc.l2tabs.tabs.core.TabToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = "l2hostility_tweaks")
public class ClientEventHandler {

	public static TabToken<TabPlayerTraits> TAB_PLAYER_TRAITS;

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() ->
			TAB_PLAYER_TRAITS = TabRegistry.registerTab(2000, TabPlayerTraits::new,
					() -> BuiltInRegistries.ITEM.get(new ResourceLocation("l2hostility", "teleport")),
					Component.translatable("screen.l2hostility_tweaks.player_traits"))
		);
	}

	@SubscribeEvent
	public static void registerGuiOverlayEvent(RegisterGuiOverlaysEvent evt) {
		evt.registerBelow(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(),
				"l2hostility_tweaks_health_overlay", new L2HHealthOverlay());
	}

	@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "l2hostility_tweaks")
	public static class ForgeEvents {

		@SubscribeEvent
		public static void onRenderLevelStage(RenderLevelStageEvent event) {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
				L2HHealthOverlay.precomputeHudState();
			}
		}

		@SubscribeEvent
		public static void onLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
			if (event.getEntity().getMainHandItem().getItem() instanceof TraitUnloaderWand) {
				boolean reverse = event.getEntity().isShiftKeyDown();
				NetworkHandler.sendCycleToServer(reverse);
			}
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void onItemTooltip(ItemTooltipEvent event) {
			ItemStack stack = event.getItemStack();

			int lv = EnchantmentHelper.getEnchantments(stack).getOrDefault(L2HFEnchantments.REPRINT_COUNTER.get(), 0);
			if (lv > 0) {
				String descKey = "enchantment.l2hostility_tweaks.reprint_counter.desc";
				String descAnyKey = "enchantment.l2hostility_tweaks.reprint_counter.desc_any";
				String descArmorKey = "enchantment.l2hostility_tweaks.reprint_counter.desc_armor";
				for (int i = 0; i < event.getToolTip().size(); i++) {
					var comp = event.getToolTip().get(i);
					if (comp.getContents() instanceof TranslatableContents tr && tr.getKey().equals(descKey)) {
						boolean isArmor = stack.getItem() instanceof ArmorItem;
						Component tip;
						if (isArmor) {
							double reduction = L2HConfig.getAntiReprintReduction() * lv;
							Component number = Component.literal(String.format("%.0f%%", reduction * 100))
									.withStyle(ChatFormatting.AQUA);
							tip = Component.translatable(descArmorKey, number).withStyle(ChatFormatting.GRAY);
						} else if (stack.is(Items.ENCHANTED_BOOK)) {
							double reduction = Math.min(L2HConfig.getAntiReprintReduction() * lv, 0.8);
							Component number = Component.literal(String.format("%.0f%%", reduction * 100))
									.withStyle(ChatFormatting.AQUA);
							tip = Component.translatable(descKey, number).withStyle(ChatFormatting.GRAY);
						} else {
							tip = Component.translatable(descAnyKey).withStyle(ChatFormatting.GRAY);
						}
						event.getToolTip().set(i, tip);
						return;
					}
				}
			}

			if (stack.is(LHItems.HOSTILITY_ESSENCE.get())) {
				Component tip = Component.translatable("tooltip.l2hostility_tweaks.essence_use",
						LHConfig.COMMON.bottleOfCurseLevel.get()).withStyle(ChatFormatting.GRAY);
				event.getToolTip().add(tip);
			}

			if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(new ResourceLocation("l2hostility", "pocket_of_restoration"))) {
				int gluttonyLevel = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.GLUTTONY_POCKET.get(), stack);
				if (gluttonyLevel > 0 && stack.getTag() != null) {
					int sealPos = -1;
					for (int i = 0; i < event.getToolTip().size(); i++) {
						var comp = event.getToolTip().get(i);
						if (comp.getContents() instanceof TranslatableContents tr && tr.getKey().equals("l2hostility.tooltip.sealed_item")) {
							sealPos = i;
							break;
						}
					}

					if (sealPos >= 0) {
						int insertAt = sealPos + 2;
						for (int i = 1; i <= gluttonyLevel; i++) {
							String key = "UnsealRoot_" + i;
							if (!stack.getTag().contains(key)) continue;
							event.getToolTip().add(insertAt++, ItemStack.of(stack.getOrCreateTagElement(key).getCompound(SealedItem.DATA)).getHoverName());
						}
					} else {
						int descPos = -1;
						for (int i = 0; i < event.getToolTip().size(); i++) {
							var comp = event.getToolTip().get(i);
							if (comp.getContents() instanceof TranslatableContents tr && tr.getKey().equals("l2hostility.item.equipment.pocket_of_restoration")) {
								descPos = i;
								break;
							}
						}
						int insertAt = descPos >= 0 ? descPos + 1 : event.getToolTip().size();
						event.getToolTip().add(insertAt++, LangData.TOOLTIP_SEAL_DATA.get().withStyle(ChatFormatting.GRAY));
						if (stack.getTag().contains("UnsealRoot")) {
							event.getToolTip().add(insertAt++, ItemStack.of(stack.getOrCreateTagElement("UnsealRoot").getCompound(SealedItem.DATA)).getHoverName());
						}
						for (int i = 1; i <= gluttonyLevel; i++) {
							String key = "UnsealRoot_" + i;
							if (!stack.getTag().contains(key)) continue;
							event.getToolTip().add(insertAt++, ItemStack.of(stack.getOrCreateTagElement(key).getCompound(SealedItem.DATA)).getHoverName());
						}
					}
				}
			}
		}
	}
}
