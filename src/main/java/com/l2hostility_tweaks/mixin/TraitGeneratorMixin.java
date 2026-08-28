package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
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

    @Shadow
    @Final
    private MobDifficultyCollector ins;

    private static final Logger LOG = LoggerFactory.getLogger("L2HostilityFix/NbtPresetGen");

    @SuppressWarnings("unchecked")
    @Inject(method = "generate", at = @At(
            value = "INVOKE",
            target = "Ljava/util/HashMap;entrySet()Ljava/util/Set;",
            shift = At.Shift.BEFORE), require = 1)
    private void l2fix$prepareFinalTraits(CallbackInfo ci) {
        java.util.Set<String> ordinaryPresetIds =
                ((TraitGenerationHelper.PresetState) (Object) this).l2fix$getOrdinaryPresetIds();
        TraitGenerationHelper.ActivePresets activePresets =
                TraitGenerationHelper.selectActivePresets(
                        entity, mobLevel, ins, ordinaryPresetIds);
        l2fix$applyNbtPresets(traits, activePresets.nbtPresets());
        TraitGenerationHelper.applyFinalFilters(
                entity, traits, mobLevel, activePresets.protectedIds());
    }

    @Unique
    private static void l2fix$applyNbtPresets(HashMap<MobTrait, Integer> traits,
                                               java.util.List<EntityConfig.TraitBase> presets) {
        try {
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
                LOG.debug("[NbtPresetGen] Applied {} active NBT preset traits", appliedCount);
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
