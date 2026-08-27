package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.L2HostilityFix;
import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.content.TraitUnloaderWand;
import com.l2hostility_tweaks.util.ImmunityHelper;
import com.l2hostility_tweaks.util.TraitCostHelper;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import com.l2hostility_tweaks.init.L2HTweaksLang;
import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.item.tool.DetectorGlasses;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class NetworkHandler {

	private static final String PROTOCOL_VERSION = "3";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation("l2hostility_tweaks", "toggle_glow"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	private static final String TAG_GLOW_DISABLED = "DetectorGlowDisabled";

	public static void init() {
		CHANNEL.registerMessage(0, ToggleGlowPacket.class,
				ToggleGlowPacket::encode,
				ToggleGlowPacket::decode,
				ToggleGlowPacket::handle);
		CHANNEL.registerMessage(1, UnloaderCyclePacket.class,
				UnloaderCyclePacket::encode,
				UnloaderCyclePacket::decode,
				UnloaderCyclePacket::handle);
		CHANNEL.registerMessage(2, UnloadTraitPacket.class,
				UnloadTraitPacket::encode,
				UnloadTraitPacket::decode,
				UnloadTraitPacket::handle);
		CHANNEL.registerMessage(3, ToggleProtectPacket.class,
				ToggleProtectPacket::encode,
				ToggleProtectPacket::decode,
				ToggleProtectPacket::handle);
		CHANNEL.registerMessage(4, SealStateRequestPacket.class,
				SealStateRequestPacket::encode,
				SealStateRequestPacket::decode,
				SealStateRequestPacket::handle,
				Optional.of(NetworkDirection.PLAY_TO_SERVER));
		CHANNEL.registerMessage(5, SealStateSyncPacket.class,
				SealStateSyncPacket::encode,
				SealStateSyncPacket::decode,
				SealStateSyncPacket::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

	public static void sendToggleToServer(int containerId, int slotIndex) {
		CHANNEL.sendToServer(new ToggleGlowPacket(containerId, slotIndex));
	}

	public static void sendToggleProtectToServer(int containerId, int slotIndex) {
		CHANNEL.sendToServer(new ToggleProtectPacket(containerId, slotIndex));
	}

	public static void sendCycleToServer(boolean reverse) {
		CHANNEL.sendToServer(new UnloaderCyclePacket(reverse));
	}

	public static void sendUnloadToServer(String traitId, boolean unloadAll) {
		CHANNEL.sendToServer(new UnloadTraitPacket(traitId, unloadAll));
	}

	public static void requestSealStateFromServer() {
		CHANNEL.sendToServer(new SealStateRequestPacket());
	}

	public static void sendSealStateToPlayer(ServerPlayer player) {
		Map<String, Long> remainingTicks = TraitDisableHelper.snapshotSealRemainingTicks(
				player.getPersistentData(), player.level().getGameTime());
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
				new SealStateSyncPacket(remainingTicks));
	}

	public record SealStateRequestPacket() {

		public static void encode(SealStateRequestPacket msg, FriendlyByteBuf buf) {
		}

		public static SealStateRequestPacket decode(FriendlyByteBuf buf) {
			return new SealStateRequestPacket();
		}

		public static void handle(SealStateRequestPacket msg,
				Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player != null) sendSealStateToPlayer(player);
			});
			ctx.setPacketHandled(true);
		}
	}

	public record SealStateSyncPacket(Map<String, Long> remainingTicks) {

		public SealStateSyncPacket {
			remainingTicks = Map.copyOf(remainingTicks);
		}

		public static void encode(SealStateSyncPacket msg, FriendlyByteBuf buf) {
			if (msg.remainingTicks.size() > TraitDisableHelper.MAX_SEAL_STATE_ENTRIES) {
				throw new IllegalArgumentException(
						"Too many seal state entries: " + msg.remainingTicks.size());
			}
			buf.writeVarInt(msg.remainingTicks.size());
			for (Map.Entry<String, Long> entry : msg.remainingTicks.entrySet()) {
				buf.writeUtf(entry.getKey());
				buf.writeLong(entry.getValue());
			}
		}

		public static SealStateSyncPacket decode(FriendlyByteBuf buf) {
			int size = buf.readVarInt();
			if (size < 0 || size > TraitDisableHelper.MAX_SEAL_STATE_ENTRIES) {
				throw new IllegalArgumentException("Invalid seal state entry count: " + size);
			}
			Map<String, Long> remainingTicks = new LinkedHashMap<>();
			for (int i = 0; i < size; i++) {
				remainingTicks.put(buf.readUtf(), buf.readLong());
			}
			return new SealStateSyncPacket(remainingTicks);
		}

		public static void handle(SealStateSyncPacket msg,
				Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> L2HostilityFix.PROXY.receiveSealState(msg.remainingTicks));
			ctx.setPacketHandled(true);
		}
	}

	public record ToggleGlowPacket(int containerId, int slotIndex) {

		public static void encode(ToggleGlowPacket msg, FriendlyByteBuf buf) {
			buf.writeInt(msg.containerId);
			buf.writeInt(msg.slotIndex);
		}

		public static ToggleGlowPacket decode(FriendlyByteBuf buf) {
			return new ToggleGlowPacket(buf.readInt(), buf.readInt());
		}

		public static void handle(ToggleGlowPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;

				if (player.containerMenu != null && player.containerMenu.containerId == msg.containerId &&
						msg.slotIndex >= 0 && msg.slotIndex < player.containerMenu.slots.size()) {
					ItemStack stack = player.containerMenu.getSlot(msg.slotIndex).getItem();
					if (stack.getItem() instanceof DetectorGlasses) {
						boolean disabled = !stack.getOrCreateTag().getBoolean(TAG_GLOW_DISABLED);
						stack.getOrCreateTag().putBoolean(TAG_GLOW_DISABLED, disabled);
						return;
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}

	public record UnloaderCyclePacket(boolean reverse) {

		public static void encode(UnloaderCyclePacket msg, FriendlyByteBuf buf) {
			buf.writeBoolean(msg.reverse);
		}

		public static UnloaderCyclePacket decode(FriendlyByteBuf buf) {
			return new UnloaderCyclePacket(buf.readBoolean());
		}

		public static void handle(UnloaderCyclePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;

				ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
				if (!(stack.getItem() instanceof TraitUnloaderWand)) return;

				MobTrait old = TraitUnloaderWand.get(stack);
				MobTrait nextTrait = msg.reverse ? TraitUnloaderWand.prev(old) : TraitUnloaderWand.next(old);
				TraitUnloaderWand.set(stack, nextTrait);
				player.displayClientMessage(
						L2HTweaksLang.translate(
								L2HTweaksLang.SEAL_SELECTED,
								nextTrait.getDesc().copy().withStyle(ChatFormatting.AQUA)),
						true);
			});
			ctx.setPacketHandled(true);
		}
	}

	public record UnloadTraitPacket(String traitId, boolean unloadAll) {

		public static void encode(UnloadTraitPacket msg, FriendlyByteBuf buf) {
			buf.writeUtf(msg.traitId);
			buf.writeBoolean(msg.unloadAll);
		}

		public static UnloadTraitPacket decode(FriendlyByteBuf buf) {
			return new UnloadTraitPacket(buf.readUtf(), buf.readBoolean());
		}

		public static void handle(UnloadTraitPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;
				if (!MobTraitCap.HOLDER.isProper(player)) return;
				MobTraitCap cap = MobTraitCap.HOLDER.get(player);
				if (!cap.isInitialized() || cap.traits.isEmpty()) return;

				ResourceLocation traitLocation = ResourceLocation.tryParse(msg.traitId);
				if (traitLocation == null) return;
				MobTrait trait = LHTraits.TRAITS.get().getValue(traitLocation);
				if (trait == null) return;

				Integer currentLevel = cap.traits.get(trait);
				if (currentLevel == null || currentLevel == 0) return;

				int absLevel = TraitCostHelper.normalizeStoredLevel(currentLevel);
				if (absLevel == 0) return;
				if (msg.unloadAll) {
					TraitUnloaderWand.unloadGroupTrait(player, cap, trait, absLevel);
				} else {
					TraitUnloaderWand.unloadSingleTrait(player, cap, trait, absLevel);
				}
			});
			ctx.setPacketHandled(true);
		}
	}

	public record ToggleProtectPacket(int containerId, int slotIndex) {

		public static void encode(ToggleProtectPacket msg, FriendlyByteBuf buf) {
			buf.writeInt(msg.containerId);
			buf.writeInt(msg.slotIndex);
		}

		public static ToggleProtectPacket decode(FriendlyByteBuf buf) {
			return new ToggleProtectPacket(buf.readInt(), buf.readInt());
		}

		public static void handle(ToggleProtectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;

				if (player.containerMenu != null && player.containerMenu.containerId == msg.containerId &&
						msg.slotIndex >= 0 && msg.slotIndex < player.containerMenu.slots.size()) {
					ItemStack stack = player.containerMenu.getSlot(msg.slotIndex).getItem();
					if (stack.getItem() instanceof DimensionBreakerItem) {
						DimensionBreakerItem.toggleProtect(stack);
						ImmunityHelper.invalidateDimensionBreaker(player);
						return;
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}

}
