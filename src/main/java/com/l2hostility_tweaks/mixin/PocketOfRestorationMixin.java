package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.init.L2HFEnchantments;
import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import dev.xkmc.l2hostility.compat.curios.EntitySlotAccess;
import dev.xkmc.l2hostility.content.item.curio.misc.PocketOfRestoration;
import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.function.Consumer;

@Mixin(value = PocketOfRestoration.class, remap = false)
public class PocketOfRestorationMixin {

	private static final Logger LOGGER = LoggerFactory.getLogger("L2HF_AbyssPocket");

	private static final ThreadLocal<Integer> l2fix$abyssLevel = new ThreadLocal<>();
	private static final ThreadLocal<Integer> l2fix$gluttonyLevel = new ThreadLocal<>();

	@Inject(method = "curioTick", at = @At("HEAD"), remap = false, cancellable = true)
	private void l2fix$captureAndRoute(SlotContext slotContext, ItemStack stack, CallbackInfo ci) {
		var le = slotContext.entity();
		if (le.level().isClientSide) return;
		if (!le.isAlive()) return;

		int abyss = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.ABYSS_POCKET.get(), stack);
		int gluttony = EnchantmentHelper.getTagEnchantmentLevel(L2HFEnchantments.GLUTTONY_POCKET.get(), stack);
		l2fix$abyssLevel.set(abyss);
		l2fix$gluttonyLevel.set(gluttony);

		if (gluttony > 0) {
			ci.cancel();
			l2fix$runMultiSlotTick(slotContext, stack);
		}
	}

	@Inject(method = "curioTick", at = @At("RETURN"), remap = false)
	private void l2fix$cleanup(SlotContext slotContext, ItemStack stack, CallbackInfo ci) {
		l2fix$abyssLevel.remove();
		l2fix$gluttonyLevel.remove();
	}

	@Redirect(method = "curioTick", remap = false,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
	private void l2fix$extraDurability(ItemStack stack, int amount, LivingEntity entity, Consumer<LivingEntity> callback) {
		stack.hurtAndBreak(amount * (1 + l2fix$abyssLevel.get()), entity, callback);
	}

	@Redirect(method = "curioTick", remap = false,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getDamageValue()I"))
	private int l2fix$adjustGuard(ItemStack stack) {
		return stack.getDamageValue() + l2fix$abyssLevel.get();
	}

	@Inject(method = "curioTick", remap = false,
			at = @At(value = "INVOKE", target = "Ldev/xkmc/l2hostility/content/item/curio/misc/PocketOfRestoration;setData(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Ljava/lang/String;J)V", remap = false, shift = At.Shift.AFTER))
	private void l2fix$speedUp(SlotContext slotContext, ItemStack stack, CallbackInfo ci) {
		if (l2fix$abyssLevel.get() <= 0) return;
		var tag = stack.getOrCreateTagElement(PocketOfRestoration.ROOT);
		int original = tag.getInt(SealedItem.TIME);
		int reduced = original / (l2fix$abyssLevel.get() + 1);
		tag.putInt(SealedItem.TIME, reduced);
		LOGGER.info("speedUp original={} reduced={} level={}", original, reduced, l2fix$abyssLevel.get());
	}

	@Unique
	private void l2fix$runMultiSlotTick(SlotContext slotContext, ItemStack stack) {
		var le = slotContext.entity();
		int maxSlots = 1 + l2fix$gluttonyLevel.get();
		var list = CurioCompat.getItemAccess(le);
		boolean changed = false;

		for (int i = 0; i < maxSlots; i++) {
			String key = l2fix$slotKey(i);
			if (stack.getTag() == null || !stack.getTag().contains(key)) continue;

			var tag = stack.getOrCreateTagElement(key);
			long time = tag.getLong(PocketOfRestoration.START);
			int dur = tag.getInt(SealedItem.TIME);
			if (le.level().getGameTime() < time + dur) continue;

			ItemStack result = ItemStack.of(tag.getCompound(SealedItem.DATA));
			EntitySlotAccess slot = CurioCompat.decode(tag.getString(PocketOfRestoration.KEY), le);
			if (slot != null && slot.get().isEmpty()) {
				slot.set(result);
			} else if (le instanceof Player player && player.addItem(result)) {
			}
			stack.getTag().remove(key);
			changed = true;
		}

		for (var e : list) {
			if (!(e.get().getItem() instanceof SealedItem)) continue;

			int emptySlot = l2fix$findEmptySlot(stack, maxSlots);
			if (emptySlot < 0) break;

			if (stack.getDamageValue() + 1 + l2fix$abyssLevel.get() >= stack.getMaxDamage()) break;

			ItemStack sealed = e.get();
			e.set(ItemStack.EMPTY);
			String id = e.getID();
			long gameTime = le.level().getGameTime();

			stack.hurtAndBreak(1 + l2fix$abyssLevel.get(), le, x -> {});

			var data = sealed.getOrCreateTag().get(SealedItem.DATA);
			if (data == null) continue;

			String key = l2fix$slotKey(emptySlot);
			var tag = stack.getOrCreateTagElement(key);
			int origTime = sealed.getOrCreateTag().getInt(SealedItem.TIME);
			tag.putInt(SealedItem.TIME, origTime / (l2fix$abyssLevel.get() + 1));
			tag.put(SealedItem.DATA, data);
			tag.putString(PocketOfRestoration.KEY, id);
			tag.putLong(PocketOfRestoration.START, gameTime);
			changed = true;
		}

		if (changed) {
			l2fix$syncStack(slotContext, stack);
		}
	}

	@Unique
	private void l2fix$syncStack(SlotContext slotContext, ItemStack stack) {
		var le = slotContext.entity();
		var curiosInv = CuriosApi.getCuriosInventory(le).resolve();
		if (curiosInv.isPresent()) {
			var handlerOpt = curiosInv.get().getStacksHandler(slotContext.identifier());
			handlerOpt.ifPresent(handler -> {
				handler.getStacks().setStackInSlot(slotContext.index(), stack);
				LOGGER.info("sync forced for slot {}", slotContext.identifier());
			});
		}
	}

	@Unique
	private String l2fix$slotKey(int index) {
		return index == 0 ? PocketOfRestoration.ROOT : PocketOfRestoration.ROOT + "_" + index;
	}

	@Unique
	private int l2fix$findEmptySlot(ItemStack stack, int maxSlots) {
		for (int i = 0; i < maxSlots; i++) {
			String key = l2fix$slotKey(i);
			if (stack.getTag() == null || !stack.getTag().contains(key)) {
				return i;
			}
		}
		return -1;
	}
}
