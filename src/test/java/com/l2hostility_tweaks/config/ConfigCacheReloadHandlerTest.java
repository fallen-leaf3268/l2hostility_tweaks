package com.l2hostility_tweaks.config;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCacheReloadHandlerTest {

    @Test
    void ragnarokOverrideTargetsCurrentUpstreamPostHurtMethod() throws Exception {
        String ragnarok = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/RagnarokTraitMixin.java"));

        assertTrue(ragnarok.contains("@Inject(method = \"postHurtImpl\""));
        assertTrue(ragnarok.contains(
                "int level, LivingEntity attacker, LivingEntity target, CallbackInfo ci"));
        assertFalse(ragnarok.contains("@Inject(method = \"sealItems\""));
    }

    @Test
    void routesInvalidationByExactConfigSpec() throws Exception {
        Field commonCache = L2HConfig.class.getDeclaredField("parsedLevelThresholds");
        Field clientCache = ClientL2HConfig.class.getDeclaredField("parsedColorSegments");
        commonCache.setAccessible(true);
        clientCache.setAccessible(true);

        try {
            commonCache.set(null, new ArrayList<>());
            clientCache.set(null, new ArrayList<>());
            ConfigCacheReloadHandler.invalidate(L2HConfig.SPEC);
            assertNull(commonCache.get(null));
            assertNotNull(clientCache.get(null));

            commonCache.set(null, new ArrayList<>());
            clientCache.set(null, new ArrayList<>());
            ConfigCacheReloadHandler.invalidate(ClientL2HConfig.CLIENT_SPEC);
            assertNotNull(commonCache.get(null));
            assertNull(clientCache.get(null));
        } finally {
            L2HConfig.invalidateCaches();
            ClientL2HConfig.invalidateCaches();
        }
    }

    @Test
    void keepsGlobalCommonConfigAndSynchronizesDisplaySnapshotLifecycle() throws Exception {
        String config = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/config/L2HConfig.java"));
        String network = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));
        String mod = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        String client = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/ClientEventHandler.java"));

        assertTrue(config.contains("registerConfig(ModConfig.Type.COMMON, SPEC"));
        assertTrue(network.contains("PROTOCOL_VERSION = \"4\""));
        assertTrue(network.contains("DisplayConfigSyncPacket.class"));
        assertTrue(network.contains("Optional.of(NetworkDirection.PLAY_TO_CLIENT)"));
        assertTrue(network.contains("L2HConfig.installDisplaySnapshot(msg.values())"));
        assertTrue(mod.contains("NetworkHandler.sendDisplayConfigToPlayer(sp)"));
        assertTrue(mod.contains("NetworkHandler.broadcastDisplayConfig()"));
        assertTrue(client.contains("ClientPlayerNetworkEvent.LoggingOut"));
        assertTrue(client.contains("L2HConfig.clearDisplaySnapshot()"));

        for (String key : new String[]{
                "antiReprintReduction", "ragnarokCountArray", "ragnarokTimeArray",
                "ragnarokBaseTime", "killerAuraDamageArray", "killerAuraBaseDamage",
                "killerAuraIntervalArray", "killerAuraBaseInterval", "drainDamageArray",
                "drainBaseDamage", "drainDurationArray", "drainBaseDuration",
                "drainDurationMaxArray", "drainBaseDurationMax", "drainCountArray",
                "extraLegendaryIds", "exclusionEnabled", "exclusionGroups",
                "playerSelfTraitEnabled", "playerSelfTraitBalanceEnabled", "playerSelfTraitBudgetRatio",
                "playerSelfTraitCostMode", "playerTraitOverrides"}) {
            assertTrue(config.contains("\"" + key + "\""), key);
        }
    }

    @Test
    void clientDisplaysUseSyncedGettersWhileRuntimeKeepsCommonGetters() throws Exception {
        String mod = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        String tooltip = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipPipeline.java"));
        String hud = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));
        String screen = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java"));
        String symbol = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolMixin.java"));
        String selfUse = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java"));
        String ragnarok = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/RagnarokTraitMixin.java"));
        String killerAura = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixin.java"));
        String drain = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DrainTraitMixin.java"));
        String dispell = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DispellTraitMixin.java"));
        String adapting = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/AdaptingTraitMixin.java"));
        String reprint = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/ReprintTraitMixin.java"));
        String mobDesc = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitDescMixin.java"));
        String immunity = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitImmunityMixin.java"));
        String difficulty = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DifficultyScreenMixin.java"));
        String glowing = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/ClientGlowingHandlerMixin.java"));
        String invisible = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/EntityInvisibleMixin.java"));
        String compactInvisible = invisible.replaceAll("\\s+", "");
        String compactKillerAura = killerAura.replaceAll("\\s+", "");

        assertTrue(tooltip.contains("L2HConfig.getDisplayAntiReprintReduction()"));
        assertTrue(tooltip.contains("L2HConfig.getDisplayBottleOfCurseLevel()"));
        assertTrue(mod.contains("LHConfig.COMMON.bottleOfCurseLevel.get()"));
        assertTrue(hud.contains("L2HConfig.getDisplayExtraLegendaryIds()"));
        assertTrue(screen.contains("L2HConfig.isDisplayPlayerSelfTraitBalanceEnabled()"));
        assertTrue(screen.contains("L2HConfig.getDisplayPlayerSelfTraitBudgetRatio()"));
        assertTrue(screen.contains("L2HConfig.getDisplayPlayerTraitOverrides()"));
        assertTrue(screen.contains("L2HConfig.getDisplayUpgradeCost("));
        assertTrue(symbol.contains("L2HConfig.getDisplayExtraLegendaryIds()"));
        assertTrue(symbol.contains("L2HConfig.isDisplayExclusionEnabled()"));
        assertTrue(symbol.contains("L2HConfig.getDisplayExclusionGroups()"));
        assertTrue(symbol.contains("L2HConfig.getDisplayPlayerTraitOverrides()"));
        assertTrue(symbol.contains("L2HConfig.isDisplayPlayerSelfTraitEnabled()"));
        assertTrue(selfUse.contains("L2HConfig.isDisplayPlayerSelfTraitEnabled()"));

        assertTrue(ragnarok.contains("L2HConfig.getRagnarokCount(level)"));
        assertTrue(ragnarok.contains("L2HConfig.getRagnarokTime(level)"));
        assertTrue(ragnarok.contains("L2HConfig.getDisplayRagnarokCount(i)"));
        assertTrue(ragnarok.contains("L2HConfig.getDisplayRagnarokTime(i)"));
        assertTrue(killerAura.contains("L2HConfig.getKillerAuraDamage(level)"));
        assertTrue(killerAura.contains("LHConfig.COMMON.killerAuraRange.get()"));
        assertTrue(killerAura.contains("L2HConfig.getDisplayKillerAuraDamage(i)"));
        assertTrue(killerAura.contains("L2HConfig.getDisplayKillerAuraInterval(i)"));
        assertTrue(killerAura.contains("L2HConfig.getDisplayKillerAuraRange()"));
        assertTrue(compactKillerAura.contains(
                "mob.level().isClientSide()?L2HConfig.getDisplayKillerAuraRange():dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraRange.get()"));
        assertTrue(drain.contains("L2HConfig.getDrainDamage(level)"));
        assertTrue(drain.contains("L2HConfig.getDisplayDrainDamage(i)"));
        assertTrue(drain.contains("L2HConfig.getDisplayDrainDuration(i)"));
        assertTrue(drain.contains("L2HConfig.getDisplayDrainDurationMax(i)"));
        assertTrue(drain.contains("L2HConfig.getDisplayDrainCount(i)"));
        assertTrue(dispell.contains("L2HConfig.getDispellTime(level)"));
        assertTrue(dispell.contains("L2HConfig.getDisplayDispellTime(i)"));
        assertTrue(dispell.contains("L2HConfig.getDisplayDispellCount(i)"));
        assertTrue(dispell.contains("L2HConfig.isDisplayOldDispellEnabled()"));
        assertTrue(adapting.contains("L2HConfig.isDisplayAdaptiveLinearEnabled()"));
        assertTrue(adapting.contains("L2HConfig.getDisplayAdaptiveReductionPerStack()"));
        assertTrue(adapting.contains("L2HConfig.getDisplayAdaptiveMaxReduction()"));
        assertTrue(reprint.contains("L2HConfig.isDisplayReprintLinearEnabled()"));
        assertTrue(reprint.contains("L2HConfig.getDisplayReprintDamage()"));
        assertTrue(mobDesc.contains("L2HConfig.getDisplayUndyingMaxResurrections()"));
        assertTrue(mobDesc.contains("L2HConfig.getDisplayUndyingSealDuration()"));
        assertTrue(immunity.contains("L2HConfig.isDisplayOldDementorEnabled()"));
        assertTrue(difficulty.contains("L2HConfig.isDisplayLevelCapEnabled()"));
        assertTrue(difficulty.contains("L2HConfig.getDisplayLevelThresholds()"));
        assertTrue(difficulty.contains("L2HConfig.isDisplayLegendaryEnabled()"));
        assertTrue(difficulty.contains("L2HConfig.getDisplayLegendaryThresholds()"));
        assertFalse(glowing.contains("L2HConfig.isDisplayDetectorGlassesRevealEnabled()"));
        assertTrue(invisible.contains("L2HConfig.isDisplayDetectorGlassesRevealEnabled()"));
        assertTrue(invisible.contains("L2HConfig.getDisplayDetectorGlassesRange()"));
        assertTrue(compactInvisible.contains(
                "entity.level().isClientSide()?L2HConfig.isDisplayDetectorGlassesRevealEnabled():L2HConfig.isDetectorGlassesRevealEnabled()"));
        assertTrue(compactInvisible.contains(
                "entity.level().isClientSide()?L2HConfig.getDisplayDetectorGlassesRange():L2HConfig.getDetectorGlassesRange()"));
        assertTrue(mod.contains("L2HConfig.getUpstreamDisplayConfig()"));
        assertTrue(mod.contains("NetworkHandler.broadcastDisplayConfig()"));
        int initialBaseline = mod.indexOf("if (lastUpstreamDisplayConfig == null)");
        int loginSnapshot = mod.indexOf("NetworkHandler.sendDisplayConfigToPlayer(sp)");
        assertTrue(initialBaseline >= 0 && initialBaseline < loginSnapshot);
    }

    @Test
    void unloaderCycleInterceptsEveryAttackInput() throws Exception {
        String client = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/ClientEventHandler.java"));
        int handler = client.indexOf(
                "onAttackInput(InputEvent.InteractionKeyMappingTriggered event)");
        int attackGuard = client.indexOf("if (!event.isAttack()) return;", handler);
        int heldItemGuard = client.indexOf("instanceof TraitUnloaderWand", attackGuard);
        int cycle = client.indexOf("NetworkHandler.sendCycleToServer", heldItemGuard);
        int cancel = client.indexOf("event.setCanceled(true);", cycle);

        assertFalse(client.contains("PlayerInteractEvent.LeftClickEmpty"));
        assertTrue(handler >= 0);
        assertTrue(attackGuard > handler);
        assertTrue(heldItemGuard > attackGuard);
        assertTrue(cycle > heldItemGuard);
        assertTrue(cancel > cycle);
    }

    @Test
    void clientActionPacketsAreRestrictedToServerboundTraffic() throws Exception {
        String network = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));

        for (String packet : new String[]{
                "ToggleGlowPacket.class", "UnloaderCyclePacket.class",
                "UnloadTraitPacket.class", "ToggleProtectPacket.class"}) {
            int registration = network.indexOf(packet);
            int nextRegistration = network.indexOf("CHANNEL.registerMessage", registration);
            String block = network.substring(registration,
                    nextRegistration >= 0 ? nextRegistration : network.length());
            assertTrue(block.contains("Optional.of(NetworkDirection.PLAY_TO_SERVER)"), packet);
        }
    }

    @Test
    void unloadPacketRequiresServerVerifiedMainHandWandBeforeMutation() throws Exception {
        String network = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));
        int packet = network.indexOf("public record UnloadTraitPacket");
        int handler = network.indexOf("public static void handle", packet);
        int heldItemGuard = network.indexOf(
                "player.getMainHandItem().getItem() instanceof TraitUnloaderWand", handler);
        int capabilityRead = network.indexOf("MobTraitCap.HOLDER.isProper(player)", handler);
        int mutation = network.indexOf("TraitUnloaderWand.unload", handler);

        assertTrue(packet >= 0);
        assertTrue(handler > packet);
        assertTrue(heldItemGuard > handler);
        assertTrue(capabilityRead > heldItemGuard);
        assertTrue(mutation > capabilityRead);
    }
}
