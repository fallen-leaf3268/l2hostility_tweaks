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

		int diff = l2fix$getDifficulty(player);

		boolean levelCapOn = L2HConfig.isDisplayLevelCapEnabled();
		boolean legendaryOn = L2HConfig.isDisplayLegendaryEnabled();
		Pair<Component, Supplier<List<Component>>> levelCapEntry = null;
		Pair<Component, Supplier<List<Component>>> legendaryEntry = null;

		if (levelCapOn) {
			MutableComponent text;
			if (diff >= L2HConfig.getDisplayLevelCapUnlimited()) {
				text = L2HTweaksLang.translate(L2HTweaksLang.LEVEL_CAP_UNLIMITED);
			} else {
				int cap = L2HConfig.getThreshold(L2HConfig.getDisplayLevelThresholds(), diff);
				text = L2HTweaksLang.translate(L2HTweaksLang.LEVEL_CAP, cap);
			}
			levelCapEntry = Pair.of(text, () -> java.util.List.of());
		}

		if (legendaryOn) {
			MutableComponent text;
			if (diff >= L2HConfig.getDisplayLegendaryUnlimited()) {
				text = L2HTweaksLang.translate(L2HTweaksLang.LEGENDARY_UNLIMITED);
			} else {
				List<int[]> thresholds = L2HConfig.getDisplayLegendaryThresholds();
				if (thresholds.isEmpty()) {
					text = L2HTweaksLang.translate(L2HTweaksLang.LEGENDARY_PRESET);
				} else {
					int limit = L2HConfig.getThreshold(thresholds, diff);
					text = L2HTweaksLang.translate(L2HTweaksLang.LEGENDARY_COUNT, limit);
				}
			}
			legendaryEntry = Pair.of(text, () -> java.util.List.of());
		}

		l2fix$replaceRankCapEntries(list, levelCapEntry, legendaryEntry);
	}

	private static <T> void l2fix$replaceRankCapEntries(List<T> list, T levelCapEntry, T legendaryEntry) {
		if (levelCapEntry == null && legendaryEntry == null) return;
		if (RANK_CAP_IDX < list.size()) {
			list.remove(RANK_CAP_IDX);
		}
		int insertIndex = Math.min(RANK_CAP_IDX, list.size());
		if (levelCapEntry != null) {
			list.add(insertIndex++, levelCapEntry);
		}
		if (legendaryEntry != null) {
			list.add(insertIndex, legendaryEntry);
		}
	}

	private static int l2fix$getDifficulty(Player player) {
		PlayerDifficulty playerDiff = PlayerDifficulty.HOLDER.get(player);
		DifficultyLevel diffLevel = playerDiff.getLevel();
		return diffLevel.getLevel();
	}
}
