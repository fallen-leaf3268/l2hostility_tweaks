package com.l2hostility_tweaks;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import com.l2hostility_tweaks.compat.kubejs.SpellDamageFlags;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.content.RingDamageListener;
import dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler;
import com.l2hostility_tweaks.init.L2HFEnchantments;
import com.l2hostility_tweaks.init.L2HFItems;

import com.l2hostility_tweaks.network.NetworkHandler;
import dev.xkmc.l2complements.content.feature.CurioFeaturePredicate;
import dev.xkmc.l2complements.content.feature.EntityFeature;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.registrate.LHItems;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.SlotTypeMessage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Mod("l2hostility_tweaks")
public class L2HostilityFix {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:main");
    public static com.l2hostility_tweaks.proxy.IProxy PROXY;
    private static final java.util.Map<java.util.UUID, java.util.LinkedHashMap<dev.xkmc.l2hostility.content.traits.base.MobTrait, Integer>> deathSnapshots = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, java.util.Map<String, Long>> deathSealExpiry = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, int[]> deathMeta = new java.util.HashMap<>();
    private static final Set<java.util.UUID> pendingTraitSync = Collections.synchronizedSet(new HashSet<>());

    public L2HostilityFix() {
        PROXY = net.minecraftforge.fml.DistExecutor.safeRunForDist(
            () -> com.l2hostility_tweaks.proxy.ClientProxy::new,
            () -> com.l2hostility_tweaks.proxy.ServerProxy::new);
        L2HConfig.init();
        NetworkHandler.init();
        L2HFItems.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientL2HConfig.CLIENT_SPEC, "l2_configs/l2hostility_tweaks-client.toml");
        L2HFEnchantments.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onBuildCreativeTab);
        MinecraftForge.EVENT_BUS.register(this);
        AttackEventHandler.register(4500, new RingDamageListener());
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE,
                () -> new SlotTypeMessage.Builder("belt")
                        .size(1)
                        .icon(new ResourceLocation("curios", "slot/empty_belt_slot"))
                        .priority(180)
                        .build());
        EntityFeature.STABLE_BODY.add(new CurioFeaturePredicate(() -> L2HFItems.TRANQUIL_BELT.get()));
    }

	private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey().location().equals(new ResourceLocation("l2library", "hostility"))) {
			event.accept(L2HFItems.SEAL_SYMBOL.get());
		}
	}

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        var self = event.getEntity();
        if (self.level().isClientSide()) return;
        if ((self.tickCount + self.getId()) % 20 != 0) return;
        var data = self.getPersistentData();
        java.util.List<String> toRemove = null;
        long gameTime = self.level().getGameTime();
        boolean hasCap = dev.xkmc.l2hostility.content.capability.mob.MobTraitCap.HOLDER.isProper(self);
        var cap = hasCap ? dev.xkmc.l2hostility.content.capability.mob.MobTraitCap.HOLDER.get(self) : null;
        for (String key : data.getAllKeys()) {
            if (!key.startsWith(com.l2hostility_tweaks.util.TraitDisableHelper.SEAL_EXPIRY_PREFIX)) continue;
            String traitId = key.substring(com.l2hostility_tweaks.util.TraitDisableHelper.SEAL_EXPIRY_PREFIX.length());
            boolean traitGone = cap == null || cap.traits.keySet().stream().noneMatch(t -> t.getID().equals(traitId));
            if (traitGone) {
                if (toRemove == null) toRemove = new java.util.ArrayList<>();
                toRemove.add(key);
                continue;
            }
            long expiry = data.getLong(key);
            if (expiry <= 0) continue;
            if (gameTime >= expiry) {
                if (toRemove == null) toRemove = new java.util.ArrayList<>();
                toRemove.add(key);
            }
        }
        if (toRemove != null) {
            for (String key : toRemove) {
                String traitId = key.substring(com.l2hostility_tweaks.util.TraitDisableHelper.SEAL_EXPIRY_PREFIX.length());
                data.remove(key);
                if (cap != null) {
                    com.l2hostility_tweaks.util.TraitDisableHelper.setDisabled(self, traitId, false);
                }
            }
        }
    }
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MobTraitCap.HOLDER.isProper(player)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(player);
        if (cap.traits.isEmpty()) return;
        java.util.LinkedHashMap<dev.xkmc.l2hostility.content.traits.base.MobTrait, Integer> snapshot = new java.util.LinkedHashMap<>();
        for (var entry : cap.traits.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue());
        }
        deathSnapshots.put(player.getUUID(), snapshot);
        deathMeta.put(player.getUUID(), new int[]{cap.lv, cap.fullDrop ? 1 : 0});

        java.util.Map<String, Long> seals = new java.util.HashMap<>();
        for (String key : player.getPersistentData().getAllKeys()) {
            if (key.startsWith(TraitDisableHelper.SEAL_EXPIRY_PREFIX)) {
                seals.put(key, player.getPersistentData().getLong(key));
            }
        }
        if (!seals.isEmpty()) {
            deathSealExpiry.put(player.getUUID(), seals);
        }
        LOGGER.info("DEATH: snapshotted {} traits + {} seals for player={} uuid={}", snapshot.size(), seals.size(), player.getName().getString(), player.getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        SpellDamageFlags.clear();
        deathSnapshots.clear();
        deathSealExpiry.clear();
        deathMeta.clear();
        pendingTraitSync.clear();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (MobTraitCap.HOLDER.isProper(sp)) {
                MobTraitCap cap = MobTraitCap.HOLDER.get(sp);
                if (cap.isInitialized()) {
                    cap.syncToPlayer(sp, sp);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        java.util.UUID uuid = event.getEntity().getUUID();
        deathSnapshots.remove(uuid);
        deathSealExpiry.remove(uuid);
        deathMeta.remove(uuid);
        pendingTraitSync.remove(uuid);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingTraitSync.isEmpty()) return;
        Iterator<java.util.UUID> it = pendingTraitSync.iterator();
        while (it.hasNext()) {
            java.util.UUID uuid = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || player.isRemoved()) {
                it.remove();
                continue;
            }
            if (MobTraitCap.HOLDER.isProper(player)) {
                MobTraitCap cap = MobTraitCap.HOLDER.get(player);
                cap.syncToClient(player);
                cap.syncToPlayer(player, player);
                LOGGER.info("SYNC: delayed sync for player={}", player.getName().getString());
            }
            it.remove();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            LOGGER.debug("CLONE: not a death — skip");
            return;
        }
        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
        boolean keepInv = newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        java.util.UUID uuid = oldPlayer.getUUID();
        java.util.LinkedHashMap<dev.xkmc.l2hostility.content.traits.base.MobTrait, Integer> snapshot = deathSnapshots.remove(uuid);
        java.util.Map<String, Long> seals = deathSealExpiry.remove(uuid);
        int[] meta = deathMeta.remove(uuid);
        LOGGER.info("CLONE: death clone keepInventory={} snapshot={} seals={} meta={} newTraits={}",
                keepInv,
                snapshot != null ? snapshot.size() : -1,
                seals != null ? seals.size() : -1,
                meta != null ? ("lv=" + meta[0] + " fullDrop=" + meta[1]) : "null",
                MobTraitCap.HOLDER.isProper(newPlayer) ? MobTraitCap.HOLDER.get(newPlayer).traits.size() : -1);

        if (!keepInv) {
            LOGGER.info("CLONE: keepInventory=false — clearing traits from new player");
            if (MobTraitCap.HOLDER.isProper(newPlayer)) {
                MobTraitCap newCap = MobTraitCap.HOLDER.get(newPlayer);
                newCap.traits.clear();
                newCap.syncToClient(newPlayer);
                newCap.syncToPlayer(newPlayer, newPlayer);
            }
            return;
        }
        if (snapshot == null || snapshot.isEmpty()) {
            LOGGER.warn("CLONE: no death snapshot for player={}", oldPlayer.getName().getString());
            return;
        }
        if (!MobTraitCap.HOLDER.isProper(newPlayer)) {
            LOGGER.warn("CLONE: HOLDER.isProper failed for new player");
            return;
        }

        MobTraitCap newCap = MobTraitCap.HOLDER.get(newPlayer);
        LOGGER.info("CLONE: copying {} traits from snapshot", snapshot.size());
        newCap.traits.clear();
        for (var entry : snapshot.entrySet()) {
            newCap.traits.put(entry.getKey(), entry.getValue());
        }
        if (meta != null) {
            newCap.lv = meta[0];
            newCap.fullDrop = meta[1] != 0;
            LOGGER.info("CLONE: restored lv={} fullDrop={}", meta[0], meta[1] != 0);
        }
        if (seals != null) {
            for (var entry : seals.entrySet()) {
                newPlayer.getPersistentData().putLong(entry.getKey(), entry.getValue());
            }
            LOGGER.info("CLONE: restored {} seal expiry entries", seals.size());
        }

        for (var entry : newCap.traits.entrySet()) {
            int level = entry.getValue();
            int absLevel = Math.abs(level);
            TraitDisableHelper.syncSealedLevelData(
                    newPlayer.getPersistentData(), entry.getKey().getID(), level);
            if (level < 0) {
                entry.getKey().initialize(newPlayer, 0);
            } else {
                entry.getKey().initialize(newPlayer, absLevel);
                entry.getKey().postInit(newPlayer, absLevel);
            }
        }

        newPlayer.setHealth(newPlayer.getMaxHealth());
        pendingTraitSync.add(uuid);
        LOGGER.info("CLONE: traits preserved — newTraits={} lv={} fullDrop={} (sync delayed)", newCap.traits, newCap.lv, newCap.fullDrop);
    }

	@SubscribeEvent
	public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		float speed = DimensionBreakerItem.getSpeed(event.getEntity(), event.getState());
		if (speed > 0) {
			event.setNewSpeed(Math.max(event.getNewSpeed(), speed));
		}
		if (DimensionBreakerItem.isProtectActive(event.getEntity())) {
			BlockPos pos = event.getPosition().orElse(event.getEntity().blockPosition());
			float hardness = event.getState().getDestroySpeed(event.getEntity().level(), pos);
			if (hardness > 0) {
				event.setNewSpeed(Math.min(event.getNewSpeed(), hardness * 3.0F));
			}
		}
	}

	@SubscribeEvent
	public void onHarvestCheck(PlayerEvent.HarvestCheck event) {
		if (DimensionBreakerItem.canHarvest(event.getEntity(), event.getTargetBlock())) {
			event.setCanHarvest(true);
		}
	}

	@SubscribeEvent
	public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof LivingEntity target)) return;
		Player player = event.getEntity();
		if (player.level().isClientSide()) return;
		ItemStack stack = event.getItemStack();
		if (!stack.is(LHItems.HOSTILITY_ESSENCE.get())) return;
		if (!MobTraitCap.HOLDER.isProper(target)) return;

		MobTraitCap cap = MobTraitCap.HOLDER.get(target);
		boolean isCreative = player.getAbilities().instabuild;

		if (!isCreative) {
			boolean isMinion = cap.minion && cap.asMinion != null
					&& cap.asMinion.uuid != null
					&& cap.asMinion.uuid.equals(player.getUUID());
			if (!isMinion) return;
		}

		int inc = LHConfig.COMMON.bottleOfCurseLevel.get();
		if (inc <= 0) return;
		cap.setLevel(target, cap.lv + inc);
		cap.syncToClient(target);
		target.setHealth(target.getMaxHealth());

		if (!isCreative) {
			stack.shrink(1);
		}
		event.setCancellationResult(InteractionResult.SUCCESS);
		event.setCanceled(true);
	}

}
