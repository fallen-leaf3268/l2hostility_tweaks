package com.l2hostility_tweaks.client;

import dev.xkmc.l2tabs.tabs.core.BaseTab;
import dev.xkmc.l2tabs.tabs.core.TabManager;
import dev.xkmc.l2tabs.tabs.core.TabToken;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class TabPlayerTraits extends BaseTab<TabPlayerTraits> {

	public TabPlayerTraits(TabToken<TabPlayerTraits> token, TabManager manager, ItemStack stack, Component title) {
		super(token, manager, stack, title);
	}

	@Override
	public void onTabClicked() {
		Minecraft.getInstance().setScreen(new PlayerTraitScreen());
	}
}
