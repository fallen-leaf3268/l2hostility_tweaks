package com.l2hostilityfix.mixin;

import com.l2hostilityfix.config.L2HConfig;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.content.logic.DifficultyLevel;
import dev.xkmc.l2hostility.content.menu.tab.DifficultyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = DifficultyScreen.class, remap = false)
public class DifficultyScreenMixin {

    @Redirect(method = "addDifficultyInfo",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 2))
    private static boolean l2fix$replaceRankCap(List<Pair<Component, Supplier<List<Component>>>> list, Object element) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return true;

        int diff = getDifficulty(player);

        // Level cap line
        if (L2HConfig.COMMON.levelCapEnabled.get()) {
            MutableComponent text;
            if (diff >= L2HConfig.COMMON.levelCapUnlimited.get()) {
                text = Component.literal("词条等级上限: 无限制");
            } else {
                int cap = L2HConfig.getThreshold(L2HConfig.getLevelThresholds(), diff);
                text = Component.literal("词条等级上限: " + cap + " 级");
            }
            list.add(Pair.of(text, null));
        }

        // Legendary limit line — right below level cap
        if (L2HConfig.COMMON.legendaryEnabled.get()) {
            MutableComponent text;
            if (diff >= L2HConfig.COMMON.legendaryUnlimited.get()) {
                text = Component.literal("传奇词条数量上限: 无限制");
            } else {
                List<int[]> thresholds = L2HConfig.getLegendaryThresholds();
                if (thresholds.isEmpty()) {
                    text = Component.literal("传奇词条数量上限: 由预设词条控制");
                } else {
                    int limit = L2HConfig.getThreshold(thresholds, diff);
                    text = Component.literal("传奇词条数量上限: " + limit + " 个");
                }
            }
            list.add(Pair.of(text, null));
        }

        boolean anyEnabled = L2HConfig.COMMON.levelCapEnabled.get()
                || L2HConfig.COMMON.legendaryEnabled.get();
        return !anyEnabled;
    }

    private static int getDifficulty(Player player) {
        PlayerDifficulty playerDiff = PlayerDifficulty.HOLDER.get(player);
        DifficultyLevel diffLevel = playerDiff.getLevel();
        return diffLevel.getLevel();
    }
}
