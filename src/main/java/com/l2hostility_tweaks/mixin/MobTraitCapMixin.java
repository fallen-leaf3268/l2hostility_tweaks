package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.capability.chunk.RegionalDifficultyModifier;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.L2Hostility;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.data.LangData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(value = MobTraitCap.class, remap = false)
public class MobTraitCapMixin {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:trait_cap");
	private static final ResourceLocation NBT_CONDITION_ID = new ResourceLocation("l2hostility_tweaks", "nbt");

	@Shadow(remap = false)
	private EntityConfig.Config configCache;

	@Shadow(remap = false)
	public int lv;

	@Shadow(remap = false)
	public boolean fullDrop;

	@Shadow(remap = false)
	public LinkedHashMap<MobTrait, Integer> traits;

	@Inject(method = "getConfigCache", at = @At("RETURN"), cancellable = true)
	private void checkNbtConditions(LivingEntity entity, CallbackInfoReturnable<EntityConfig.Config> cir) {
		EntityConfig merged = (EntityConfig) L2Hostility.ENTITY.getMerged();
		EntityConfig.Config nbtConfig = merged.get(entity.getType(), NBT_CONDITION_ID, LivingEntity.class, entity);
		if (nbtConfig != null) {
			configCache = nbtConfig;
			cir.setReturnValue(nbtConfig);
		}
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void l2fix$zeroMobLevel(Level level, LivingEntity entity, RegionalDifficultyModifier modifier, CallbackInfo ci) {
		if (L2HConfig.isDisableMobLevel()) {
			this.lv = 0;
		}
	}

	@Inject(method = "getTitle", at = @At("RETURN"), cancellable = true, remap = false)
	private void l2fix$fixDisabledTitle(boolean showLevel, boolean showTrait, CallbackInfoReturnable<List<Component>> cir) {
		LOGGER.debug("getTitle showLevel={} showTrait={} lv={} fullDrop={} traits={}", showLevel, showTrait, lv, fullDrop, traits);
		if (!showTrait) return;
		boolean hasDisabled = false;
		for (var entry : traits.entrySet()) {
			if (entry.getValue() < 0) {
				hasDisabled = true;
				break;
			}
		}
		if (!hasDisabled) return;

		List<Component> ans = new ArrayList<>();
		if (showLevel && lv > 0) {
			ans.add(LangData.LV.get(lv).withStyle(Style.EMPTY
					.withColor(fullDrop ? LHConfig.CLIENT.overHeadLevelColorAbyss.get()
							: LHConfig.CLIENT.overHeadLevelColor.get())));
		}
		MutableComponent temp = null;
		int count = 0;
		for (var e : traits.entrySet()) {
			int level = e.getValue();
			MutableComponent comp;
			if (level < 0) {
				comp = e.getKey().getFullDesc(-level);
				comp = comp.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
			} else {
				comp = e.getKey().getFullDesc(level);
			}
			if (temp == null) {
				temp = Component.empty().append(comp);
				count = 1;
			} else {
				temp.append(Component.literal(" / ").withStyle(ChatFormatting.WHITE)).append(comp);
				count++;
				if (count >= 3) {
					ans.add(temp);
					count = 0;
					temp = null;
				}
			}
		}
		if (temp != null) ans.add(temp);
		cir.setReturnValue(ans);
	}

	@Redirect(method = "traitEvent", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V"), remap = false)
	private void l2fix$filterTraitEvent(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		map.forEach((k, v) -> {
			if (v > 0) cons.accept(k, v);
		});
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V", ordinal = 0), remap = false)
	private void l2fix$filterPostInit(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		map.forEach((k, v) -> {
			if (v > 0) cons.accept(k, v);
		});
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V", ordinal = 1), remap = false)
	private void l2fix$filterTick(LinkedHashMap<MobTrait, Integer> map, BiConsumer<MobTrait, Integer> cons) {
		map.forEach((k, v) -> {
			if (v > 0) cons.accept(k, v);
		});
	}
}
