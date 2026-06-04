package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import dev.xkmc.l2hostility.compat.curios.EntitySlotAccess;
import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import dev.xkmc.l2hostility.content.traits.legendary.RagnarokTrait;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.data.LHTagGen;
import dev.xkmc.l2hostility.init.registrate.LHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(value = RagnarokTrait.class, remap = false)
public class RagnarokTraitMixin {

	private static boolean l2fix$allowSeal(EntitySlotAccess access) {
		ItemStack stack = access.get();
		if (stack.isEmpty()) return false;
		if (stack.is(LHItems.SEAL.get())) return false;
		if (stack.is(LHTagGen.NO_SEAL)) return false;
		if (!LHConfig.COMMON.ragnarokSealBackpack.get()) {
			var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
			if (rl == null) return false;
			if (rl.toString().contains("backpack")) return false;
		}
		if (!LHConfig.COMMON.ragnarokSealSlotAdder.get()) {
			return !CurioCompat.isSlotAdder(access);
		}
		return true;
	}

	@Inject(method = "sealItems", at = @At("HEAD"), cancellable = true)
	private void l2fix$ragnarokSeal(int level, LivingEntity target, CallbackInfo ci) {
		int count = L2HConfig.getRagnarokCount(level);
		int time = L2HConfig.getRagnarokTime(level);
		List<EntitySlotAccess> list = new ArrayList<>(CurioCompat.getItemAccess(target)
				.stream().filter(RagnarokTraitMixin::l2fix$allowSeal).toList());
		count = Math.min(count, list.size());
		for (int i = 0; i < count; i++) {
			int index = target.getRandom().nextInt(list.size());
			EntitySlotAccess slot = list.remove(index);
			slot.modify(e -> SealedItem.sealItem(e, time));
		}
		ci.cancel();
	}

	@Redirect(method = "addDetail", at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
	private boolean l2fix$ragnarokDetail(List<Component> list, Object component) {
		int max = ((RagnarokTrait) (Object) this).getMaxLevel();
		Component countText = l2fix$mapLevel(i -> Component.literal(L2HConfig.getRagnarokCount(i) + "")
				.withStyle(ChatFormatting.AQUA), max);
		Component timeText = l2fix$mapLevel(i -> Component.literal(Math.round(L2HConfig.getRagnarokTime(i) / 20f) + "")
				.withStyle(ChatFormatting.AQUA), max);
		return list.add(Component.translatable(((RagnarokTrait) (Object) this).getDescriptionId() + ".desc",
				countText, timeText).withStyle(ChatFormatting.GRAY));
	}

	private static Component l2fix$mapLevel(Function<Integer, Component> func, int max) {
		Component comp = null;
		for (int i = 1; i <= max; i++) {
			Component part = func.apply(i);
			comp = comp == null ? part : comp.copy().append(Component.literal("/").withStyle(ChatFormatting.GRAY)).append(part);
		}
		return comp;
	}
}
