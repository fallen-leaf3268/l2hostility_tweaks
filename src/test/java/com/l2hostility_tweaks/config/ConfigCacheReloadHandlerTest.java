package com.l2hostility_tweaks.config;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import com.l2hostility_tweaks.mixin.MixinTestInvoker;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ConfigCacheReloadHandlerTest {

    @Test
    void hudStateSeparatesTraitsHealthAndVanillaBossSuppression() throws Exception {
        Class<?> overlay = Class.forName("com.l2hostility_tweaks.client.L2HHealthOverlay");
        Method resolve = overlay.getDeclaredMethod(
                "l2fix$resolveHudState", boolean.class, boolean.class, boolean.class);
        resolve.setAccessible(true);
        for (boolean valid : new boolean[]{false, true}) {
            for (boolean boss : new boolean[]{false, true}) {
                for (boolean preference : new boolean[]{false, true}) {
                    Object state = resolve.invoke(null, valid, boss, preference);
                    assertEquals(valid, hudStateFlag(state, "showTraits"),
                            "traits: valid=" + valid + ", boss=" + boss + ", preference=" + preference);
                    boolean expectedHealth = valid && !(preference && boss);
                    assertEquals(expectedHealth, hudStateFlag(state, "showHealth"),
                            "health: valid=" + valid + ", boss=" + boss + ", preference=" + preference);
                    assertEquals(expectedHealth, hudStateFlag(state, "hideBossBars"),
                            "boss: valid=" + valid + ", boss=" + boss + ", preference=" + preference);
                }
            }
        }
    }

    private static boolean hudStateFlag(Object state, String name) throws Exception {
        Method accessor = state.getClass().getDeclaredMethod(name);
        accessor.setAccessible(true);
        return (boolean) accessor.invoke(state);
    }

    @Test
    void traitsOnlyRenderPathSkipsHealthAndHeaderButStillDrawsTraitLines() throws Exception {
        var owner = MixinTestInvoker.bytecode("com/l2hostility_tweaks/client/L2HHealthOverlay");
        var publicRender = owner.methods.stream().filter(method -> method.name.equals("render"))
                .findFirst().orElseThrow();
        MethodInsnNode traitsFlag = findMethodCall(publicRender.instructions.getFirst(), "showTraits");
        assertNotNull(traitsFlag, "outer render path must branch on showTraits instead of health visibility");
        AbstractInsnNode traitsBranchInstruction = nextExecutable(traitsFlag);
        JumpInsnNode traitsBranch = assertInstanceOf(JumpInsnNode.class, traitsBranchInstruction);
        assertEquals(org.objectweb.asm.Opcodes.IFEQ, traitsBranch.getOpcode());
        assertTrue(methodCallsUntilReturn(traitsBranch.getNext()).contains("renderTargetHud"));

        var render = owner.methods.stream().filter(method -> method.name.equals("renderTargetHud"))
                .findFirst().orElseThrow();
        MethodInsnNode healthFlag = findMethodCall(render.instructions.getFirst(), "showHealth");
        assertNotNull(healthFlag, "render path must branch on showHealth");
        AbstractInsnNode branchInstruction = nextExecutable(healthFlag);
        JumpInsnNode branch = assertInstanceOf(JumpInsnNode.class, branchInstruction);
        assertEquals(org.objectweb.asm.Opcodes.IFEQ, branch.getOpcode());

        List<String> traitsOnlyCalls = methodCallsUntilReturn(branch.label);
        assertTrue(traitsOnlyCalls.contains("renderTraitLines"));
        assertFalse(traitsOnlyCalls.contains("renderHeader"));
        assertFalse(traitsOnlyCalls.contains("fillRun"));

        List<String> healthCalls = methodCallsUntilReturn(branch.getNext());
        assertTrue(healthCalls.contains("renderHeader"));
        assertTrue(healthCalls.contains("renderTraitLines"));
        assertTrue(healthCalls.contains("fillRun"));
    }

    private static MethodInsnNode findMethodCall(AbstractInsnNode start, String name) {
        for (AbstractInsnNode instruction = start; instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name)) return call;
        }
        return null;
    }

    private static AbstractInsnNode nextExecutable(AbstractInsnNode instruction) {
        do {
            instruction = instruction.getNext();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private static List<String> methodCallsUntilReturn(AbstractInsnNode start) {
        List<String> calls = new ArrayList<>();
        for (AbstractInsnNode instruction = start; instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call) calls.add(call.name);
            if (instruction.getOpcode() == org.objectweb.asm.Opcodes.RETURN) break;
        }
        return calls;
    }

    @Test
    void manualSymbolEntrypointsCheckSharedConflictBeforeEarlyReturnsAndMutation() throws Exception {
        assertConflictCheckOrdering(
                "com/l2hostility_tweaks/mixin/TraitSymbolMixin",
                "l2fix$fixSealedLevel", List.of("get", "shrink"));
        assertConflictCheckOrdering(
                "com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin",
                "l2fix$traitSymbolSelfUse", List.of("get", "compute", "shrink"));
    }

    private static void assertConflictCheckOrdering(
            String ownerName, String callbackName, List<String> laterCalls) throws Exception {
        var owner = MixinTestInvoker.bytecode(ownerName);
        var callback = owner.methods.stream().filter(method -> method.name.equals(callbackName))
                .findFirst().orElseThrow();
        int exclusionEnabled = -1;
        int sharedConflict = -1;
        int firstLaterCall = Integer.MAX_VALUE;
        int index = 0;
        for (var instruction = callback.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext(), index++) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.name.equals("isExclusionEnabled")) exclusionEnabled = index;
            if (call.name.equals("findExclusionConflict")) sharedConflict = index;
            boolean stateAccess = call.owner.startsWith("java/util/") && laterCalls.contains(call.name);
            boolean consumption = call.owner.equals("net/minecraft/world/item/ItemStack")
                    && call.name.equals("shrink");
            if (stateAccess || consumption) {
                firstLaterCall = Math.min(firstLaterCall, index);
            }
        }
        assertTrue(exclusionEnabled >= 0, callbackName + " config gate");
        assertTrue(sharedConflict > exclusionEnabled, callbackName + " shared conflict call");
        assertTrue(sharedConflict < firstLaterCall, callbackName + " conflict must precede rank reads/mutation/consumption");
    }

    @Test
    void hiddenBalanceSuppressesOverrideHintAndEmptyLegendaryThresholdDisplaysOne() throws Exception {
        String symbol = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolMixin.java"));
        String difficulty = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DifficultyScreenMixin.java"));
        String compactSymbol = symbol.replaceAll("\\s+", "");

        assertTrue(compactSymbol.contains(
                "if(L2HConfig.isDisplayPlayerSelfTraitBalanceEnabled()&&override!=null)"));
        assertEquals(1, L2HConfig.getThreshold(List.of(), 200));
        assertTrue(difficulty.contains(
                "L2HConfig.getThreshold(L2HConfig.getDisplayLegendaryThresholds(), diff)"));
        assertFalse(difficulty.contains("L2HTweaksLang.LEGENDARY_PRESET"));
    }

    @Test
    void undyingCountOnlyModeStillExhaustsAndHasDedicatedTooltip() throws Exception {
        assertTrue(com.l2hostility_tweaks.util.TraitDisableHelper.isUndyingLimitExhausted(2, 2, 0));
        assertFalse(com.l2hostility_tweaks.util.TraitDisableHelper.isUndyingLimitExhausted(2, 1, 0));
        assertFalse(com.l2hostility_tweaks.util.TraitDisableHelper.isUndyingLimitExhausted(-1, 100, 0));
        assertTrue(com.l2hostility_tweaks.util.TraitDisableHelper.isUndyingLimitExhausted(0, 0, 0));
        assertUndyingTooltip(0, "limit_count_only", 2);
        assertUndyingTooltip(20, "limit_timed", 2, 20);
        assertUndyingTooltip(-1, "limit_permanent", 2);
        assertTrue(Files.readString(Path.of("src/main/resources/assets/l2hostility_tweaks/lang/zh_cn.json"))
                .contains("最多触发 %s 次复活"));
        assertTrue(Files.readString(Path.of("src/main/resources/assets/l2hostility_tweaks/lang/en_us.json"))
                .contains("Allows at most %s resurrections"));
    }

    private static void assertUndyingTooltip(int duration, String suffix, Object... arguments) {
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class,
                TraitDisableHelper.buildUndyingLimitDetail(2, duration).getContents());
        assertEquals("trait.l2hostility_tweaks.undying." + suffix, contents.getKey());
        assertArrayEquals(arguments, contents.getArgs());
    }

    @Test
    void actualUndyingCallbacksSkipSealingAtZeroDuration() throws Exception {
        var owner = MixinTestInvoker.bytecode("com/l2hostility_tweaks/mixin/UndyingTraitMixin");
        for (String name : List.of("l2fix$limitResurrections", "l2fix$incrementCount")) {
            var callback = owner.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            var inject = callback.visibleAnnotations.stream().filter(a -> a.desc.equals(
                    "Lorg/spongepowered/asm/mixin/injection/Inject;")).findFirst().orElseThrow();
            assertEquals(List.of("onDeath"), inject.values.get(inject.values.indexOf("method") + 1));
            for (int duration : new int[]{0, 20, -1}) {
                for (int count : new int[]{0, 2}) {
                    Object entity = new Object(), level = new Object(), data = new Object(), event = new Object();
                    CallbackInfo ci = new CallbackInfo("onDeath", true);
                    int[] seals = {0};
                    MixinTestInvoker.replay(owner, callback, (callOwner, call, args) -> {
                        switch (call) {
                            case "level": assertSame(entity, args.get(0)); return level;
                            case "isClientSide": assertSame(level, args.get(0)); return false;
                            case "isDisabled": assertSame(entity, args.get(0)); return false;
                            case "getUndyingMaxResurrections": return 2;
                            case "getUndyingSealDuration": return duration;
                            case "getPersistentData": assertSame(entity, args.get(0)); return data;
                            case "getInt": assertEquals(List.of(data, TraitDisableHelper.UNDYING_COUNT_KEY), args); return count;
                            case "isUndyingLimitExhausted":
                                assertEquals(List.of(2, count, duration), args);
                                return TraitDisableHelper.isUndyingLimitExhausted(2, count, duration);
                            case "l2fix$sealUndying": assertEquals(List.of(entity, duration), args); seals[0]++; return null;
                            case "cancel": assertSame(ci, args.get(0)); ci.cancel(); return null;
                            case "isCanceled": assertSame(event, args.get(0)); return true;
                            case "syncUndyingCountData": assertEquals(List.of(data, count + 1), args); return null;
                            default: throw new AssertionError("Unexpected Undying dependency: " + callOwner + "." + call);
                        }
                    }, new Object(), 1, entity, event, ci);
                    assertEquals(count >= 2 && duration != 0 ? 1 : 0, seals[0], name + " duration=" + duration);
                    assertEquals(name.equals("l2fix$limitResurrections") && count >= 2, ci.isCancelled());
                }
            }
        }
    }

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
        assertTrue(network.contains("PROTOCOL_VERSION = \"7\""));
        assertTrue(network.contains("DisplayConfigSyncPacket.class"));
        assertTrue(network.contains("Optional.of(NetworkDirection.PLAY_TO_CLIENT)"));
        assertTrue(network.contains("L2HConfig.installDisplaySnapshot(msg.values())"));
        assertTrue(mod.contains("NetworkHandler.sendDisplayConfigToPlayer(sp)"));
        assertTrue(mod.contains("NetworkHandler.broadcastDisplayConfig()"));
        assertTrue(client.contains("ClientPlayerNetworkEvent.LoggingOut"));
        assertTrue(client.contains("L2HConfig.clearDisplaySnapshot()"));
        assertTrue(client.contains("L2HostilityFix.PROXY.clearTraitSpawnIndex()"));

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
        assertTrue(hud.contains("LegendaryTraitClassifier.isDisplayLegendary("));
        assertTrue(screen.contains("L2HConfig.isDisplayPlayerSelfTraitBalanceEnabled()"));
        assertTrue(screen.contains("L2HConfig.getDisplayPlayerSelfTraitBudgetRatio()"));
        assertTrue(screen.contains("L2HConfig.getDisplayPlayerTraitOverrides()"));
        assertTrue(screen.contains("L2HConfig.getDisplayUpgradeCost("));
        assertTrue(symbol.contains("LegendaryTraitClassifier.isDisplayExtraLegendary("));
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
    void unloadPacketAllowsPanelUseWithoutTestWandAndKeepsServerValidation() throws Exception {
        String network = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));
        int packet = network.indexOf("public record UnloadTraitPacket");
        int handler = network.indexOf("public static void handle", packet);
        int nextPacket = network.indexOf("public record ToggleProtectPacket", handler);
        String handlerBody = network.substring(handler, nextPacket);
        int capabilityRead = handlerBody.indexOf("MobTraitCap.HOLDER.isProper(player)");
        int traitIdValidation = handlerBody.indexOf("ResourceLocation.tryParse(msg.traitId)");
        int ownedTraitRead = handlerBody.indexOf("cap.traits.get(trait)");
        int mutation = handlerBody.indexOf("TraitUnloaderWand.unload");

        assertTrue(packet >= 0);
        assertTrue(handler > packet);
        assertTrue(nextPacket > handler);
        assertFalse(handlerBody.contains("player.getMainHandItem()"));
        assertFalse(handlerBody.contains("instanceof TraitUnloaderWand"));
        assertTrue(capabilityRead >= 0 && capabilityRead < traitIdValidation);
        assertTrue(traitIdValidation < ownedTraitRead);
        assertTrue(ownedTraitRead < mutation);
    }
}
