package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.content.TraitUnloaderWand;
import com.l2hostility_tweaks.network.NetworkHandler;
import dev.xkmc.l2tabs.tabs.core.TabRegistry;
import dev.xkmc.l2tabs.tabs.core.TabToken;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

	}
}
