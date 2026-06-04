package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import com.l2hostility_tweaks.content.TraitUnloaderWand;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

public class NetworkHandler {

	private static final String PROTOCOL_VERSION = "1";
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
	}

	public static void sendToggleToServer() {
		CHANNEL.sendToServer(new ToggleGlowPacket());
	}

	public static void sendToggleProtectToServer() {
		CHANNEL.sendToServer(new ToggleProtectPacket());
	}

	public static void sendCycleToServer(boolean reverse) {
		CHANNEL.sendToServer(new UnloaderCyclePacket(reverse));
	}

	public static void sendUnloadToServer(String traitId, boolean unloadAll) {
		CHANNEL.sendToServer(new UnloadTraitPacket(traitId, unloadAll));
	}

	public record ToggleGlowPacket() {

		public static void encode(ToggleGlowPacket msg, FriendlyByteBuf buf) {}

		public static ToggleGlowPacket decode(FriendlyByteBuf buf) {
			return new ToggleGlowPacket();
		}

		public static void handle(ToggleGlowPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;

				for (EquipmentSlot slot : EquipmentSlot.values()) {
					ItemStack stack = player.getItemBySlot(slot);
					if (stack.getItem() instanceof DetectorGlasses) {
						boolean disabled = !stack.getOrCreateTag().getBoolean(TAG_GLOW_DISABLED);
						stack.getOrCreateTag().putBoolean(TAG_GLOW_DISABLED, disabled);
						return;
					}
				}

				List<ItemStack> curioStacks = CurioCompat.getItems(player, s -> s.getItem() instanceof DetectorGlasses);
				for (ItemStack stack : curioStacks) {
					boolean disabled = !stack.getOrCreateTag().getBoolean(TAG_GLOW_DISABLED);
					stack.getOrCreateTag().putBoolean(TAG_GLOW_DISABLED, disabled);
					return;
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

				MobTrait trait = LHTraits.TRAITS.get().getValue(new ResourceLocation(msg.traitId));
				if (trait == null) return;

				Integer currentLevel = cap.traits.get(trait);
				if (currentLevel == null || currentLevel == 0) return;

				int absLevel = Math.abs(currentLevel);
				if (msg.unloadAll) {
					TraitUnloaderWand.unloadGroupTrait(player, cap, trait, absLevel);
				} else {
					TraitUnloaderWand.unloadSingleTrait(player, cap, trait, absLevel);
				}
			});
			ctx.setPacketHandled(true);
		}
	}

	public record ToggleProtectPacket() {

		public static void encode(ToggleProtectPacket msg, FriendlyByteBuf buf) {}

		public static ToggleProtectPacket decode(FriendlyByteBuf buf) {
			return new ToggleProtectPacket();
		}

		public static void handle(ToggleProtectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;
				ItemStack stack = DimensionBreakerItem.findEquipped(player);
				if (stack.isEmpty()) return;
				DimensionBreakerItem.toggleProtect(stack);
			});
			ctx.setPacketHandled(true);
		}
	}

}
