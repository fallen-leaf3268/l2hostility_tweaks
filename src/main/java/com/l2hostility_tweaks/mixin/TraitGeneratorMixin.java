package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.L2Hostility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(value = TraitGenerator.class, remap = false)
public class TraitGeneratorMixin {

    @Shadow
    @Final
    private LivingEntity entity;

    @Shadow
    @Final
    private int mobLevel;

    @Shadow
    @Final
    private HashMap<MobTrait, Integer> traits;

    private static final Logger LOG = LoggerFactory.getLogger("L2HostilityFix/NbtPresetGen");
    private static final ResourceLocation NBT_CONDITION_ID = new ResourceLocation("l2hostility_tweaks", "nbt");

    @SuppressWarnings("unchecked")
    @Inject(method = "generate", at = @At(
            value = "INVOKE",
            target = "Ljava/util/HashMap;entrySet()Ljava/util/Set;",
            shift = At.Shift.BEFORE), require = 1)
    private void l2fix$prepareFinalTraits(CallbackInfo ci) {
        l2fix$applyNbtPresets(entity, traits);
        TraitGenerationHelper.applyFinalFilters(entity, traits, mobLevel);
    }

    @Unique
    private static void l2fix$applyNbtPresets(LivingEntity entity,
                                               HashMap<MobTrait, Integer> traits) {
        try {
            if (entity == null) return;

            EntityConfig merged = (EntityConfig) L2Hostility.ENTITY.getMerged();
            EntityConfig.Config nbtConfig = merged.get(entity.getType(), NBT_CONDITION_ID, LivingEntity.class, entity);
            if (nbtConfig == null) return;

            var presets = nbtConfig.traits();
            if (presets == null || presets.isEmpty()) return;

            if (traits == null) return;

            int appliedCount = 0;
            for (EntityConfig.TraitBase preset : presets) {
                MobTrait mt = preset.trait();
                if (mt == null) {
                    LOG.warn("[NbtPresetGen] Preset trait is unresolved, skipping");
                    continue;
                }
                int minLevel = preset.min();

                Integer existing = traits.get(mt);
                int currentRank = existing != null ? existing : 0;
                if (!l2fix$shouldApplyPreset(currentRank, minLevel)) continue;

                traits.put(mt, minLevel);
                appliedCount++;
            }
            if (appliedCount > 0) {
                LOG.debug("[NbtPresetGen] Applied {} NBT preset traits to {}", appliedCount, entity.getName().getString());
            }
        } catch (Exception e) {
            LOG.error("[NbtPresetGen] Failed to apply NBT presets", e);
        }
    }

    @Unique
    static boolean l2fix$shouldApplyPreset(int currentRank, int minLevel) {
        return currentRank < minLevel;
    }

}
