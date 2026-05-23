package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.content.logic.DifficultyLevel;
import dev.xkmc.l2hostility.content.menu.tab.DifficultyScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = DifficultyScreen.class, remap = false)
public class DifficultyScreenMixin {

	private static final int RANK_CAP_IDX = 2;

	@Inject(method = "addDifficultyInfo", at = @At("RETURN"))
	private static void l2fix$addCustomInfo(List<Pair<Component, Supplier<List<Component>>>> list, ChatFormatting[] formats, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		int diff = getDifficulty(player);

		if (RANK_CAP_IDX < list.size()) {
			list.remove(RANK_CAP_IDX);
		}

		if (L2HConfig.COMMON.levelCapEnabled.get()) {
			MutableComponent text;
			if (diff >= L2HConfig.COMMON.levelCapUnlimited.get()) {
				text = L2HTweaksLang.translate(L2HTweaksLang.LEVEL_CAP_UNLIMITED);
			} else {
				int cap = L2HConfig.getThreshold(L2HConfig.getLevelThresholds(), diff);
				text = L2HTweaksLang.translate(L2HTweaksLang.LEVEL_CAP, cap);
			}
			list.add(RANK_CAP_IDX, Pair.of(text, null));
		}

		if (L2HConfig.COMMON.legendaryEnabled.get()) {
			MutableComponent text;
			if (diff >= L2HConfig.COMMON.legendaryUnlimited.get()) {
				text = L2HTweaksLang.translate(L2HTweaksLang.LEGENDARY_UNLIMITED);
			} else {
				List<int[]> thresholds = L2HConfig.getLegendaryThresholds();
				if (thresholds.isEmpty()) {
					text = L2HTweaksLang.translate(L2HTweaksLang.LEGENDARY_PRESET);
				} else {
					int limit = L2HConfig.getThreshold(thresholds, diff);
					text = L2HTweaksLang.translate(L2HTweaksLang.LEGENDARY_COUNT, limit);
				}
			}
			list.add(RANK_CAP_IDX + 1, Pair.of(text, null));
		}
	}

	private static int getDifficulty(Player player) {
		PlayerDifficulty playerDiff = PlayerDifficulty.HOLDER.get(player);
		DifficultyLevel diffLevel = playerDiff.getLevel();
		return diffLevel.getLevel();
	}
}
