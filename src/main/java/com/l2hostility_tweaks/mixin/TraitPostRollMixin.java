package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(value = TraitGenerator.class, remap = false)
public class TraitPostRollMixin implements TraitGenerationHelper.PresetState {

    private static final Logger L2FIX$LOG = LoggerFactory.getLogger("L2HostilityFix/TraitGen");

    @Shadow
    @Final
    private LivingEntity entity;

    @Shadow
    @Final
    private int mobLevel;

    @Shadow
    @Final
    private MobDifficultyCollector ins;

    @Shadow
    @Final
    private HashMap<MobTrait, Integer> traits;

    @Shadow
    private int level;

    @Unique
    private boolean l2fix$legendaryCounted;

    @Unique
    private int l2fix$nonProtectedLegendaryCount;

    @Unique
    private Set<String> l2fix$protectedIds;

    @Unique
    private final Set<String> l2fix$appliedPresetIds = new java.util.LinkedHashSet<>();

    @Unique
    private int l2fix$mobLevel;

    @Unique
    private int l2fix$globalLevelCap;

    @Unique
    private Map<String, int[]> l2fix$perTraitCaps;

    @Unique
    private Set<String> l2fix$extraLegendaryIds;

    @Inject(method = "generateTraits", at = @At("HEAD"), cancellable = true, require = 1)
    private static void l2fix$disableAllTraitGeneration(
            MobTraitCap cap, LivingEntity entity, int mobLevel,
            HashMap<MobTrait, Integer> traits, MobDifficultyCollector ins, CallbackInfo ci) {
        if (L2HConfig.isDisableAllTraits()) ci.cancel();
    }

    // === 早期拦截：关闭非预设词条（保留 genBase 预设，跳过随机生成） ===
    @Inject(method = "generate", at = @At("HEAD"))
    private void l2fix$interceptGenerate(CallbackInfo ci) {
        if (L2HConfig.isDisableNonPresetTraits()) {
            level = 0;
        }
    }

    @Redirect(method = "genBase", at = @At(value = "INVOKE",
            target = "Ldev/xkmc/l2hostility/content/logic/TraitGenerator;setRank(Ldev/xkmc/l2hostility/content/traits/base/MobTrait;I)V"),
            require = 1)
    private void l2fix$applyPresetRank(TraitGenerator self, MobTrait trait, int newRank) {
        if (newRank > 0) l2fix$appliedPresetIds.add(trait.getID());
        l2fix$applyRank(trait, newRank);
    }

    @Inject(method = "genBase", at = @At(
            value = "INVOKE",
            target = "Ldev/xkmc/l2hostility/content/logic/TraitGenerator;setRank(Ldev/xkmc/l2hostility/content/traits/base/MobTrait;I)V",
            shift = At.Shift.AFTER), require = 1)
    private void l2fix$confirmAppliedPreset(EntityConfig.TraitBase preset, CallbackInfo ci) {
        MobTrait trait = preset.trait();
        if (trait != null && traits.containsKey(trait)) {
            l2fix$appliedPresetIds.add(trait.getID());
        }
    }

    @Override
    public Set<String> l2fix$getAppliedPresetIds() {
        return Set.copyOf(l2fix$appliedPresetIds);
    }

    @Redirect(method = "generate",
            at = @At(value = "INVOKE",
                    target = "Ldev/xkmc/l2hostility/content/logic/MobDifficultyCollector;getMaxTraitLevel()I"))
    private int l2fix$overrideMaxTraitLevel(MobDifficultyCollector ins) {
        int original = ins.getMaxTraitLevel();
        if (!L2HConfig.COMMON.levelCapEnabled.get()) return original;

        int diff = mobLevel;
        int ourCap = L2HConfig.getThreshold(L2HConfig.getLevelThresholds(), diff);

        if (diff >= L2HConfig.COMMON.levelCapUnlimited.get()) {
            L2FIX$LOG.debug("[MaxTraitLevel] diff={}, unlimited=true, result=MAX_VALUE", diff);
            return Integer.MAX_VALUE;
        }

        L2FIX$LOG.debug("[MaxTraitLevel] diff={}, original={}, ourCap={}, result={}", diff, original, ourCap, ourCap);
        return ourCap;
    }

    @Redirect(method = "generate",
            at = @At(value = "INVOKE",
                    target = "Ldev/xkmc/l2hostility/content/logic/TraitGenerator;setRank(Ldev/xkmc/l2hostility/content/traits/base/MobTrait;I)V"))
    private void l2fix$redirectSetRank(TraitGenerator self, MobTrait trait, int newRank) {
        l2fix$applyRank(trait, newRank);
    }

    @Unique
    private void l2fix$applyRank(MobTrait trait, int newRank) {
        if (newRank <= 0) return;

        int cost = ins != null ? trait.getCost(ins.trait_cost) : 1;
        int capped = newRank;

        l2fix$ensureInit();

        L2FIX$LOG.debug("[SetRank] trait={}, newRank={}, mobLevel={}, globalLevelCap={}, protectedIds={}",
                trait.getID(), newRank, l2fix$mobLevel, l2fix$globalLevelCap, l2fix$protectedIds);

        // === Level cap ===
        if (L2HConfig.COMMON.levelCapEnabled.get() && capped > 1) {
            int diff = l2fix$mobLevel;
            if (diff < L2HConfig.COMMON.levelCapUnlimited.get()
                    && !l2fix$protectedIds.contains(trait.getID())) {
                int[] custom = l2fix$perTraitCaps.get(trait.getID());
                capped = L2HConfig.applyPerTraitLevelCap(
                        capped, l2fix$globalLevelCap, diff, custom);
            }
        }

        // === Legendary limit ===
        if (L2HConfig.COMMON.legendaryEnabled.get() && capped > 0) {
            String id = trait.getID();
            boolean isLegendary = trait instanceof LegendaryTrait || l2fix$extraLegendaryIds.contains(id);
            if (isLegendary && !l2fix$protectedIds.contains(id)) {
                int diff = l2fix$mobLevel;
                if (diff < L2HConfig.COMMON.legendaryUnlimited.get()) {
                    int maxAllowed = L2HConfig.getThreshold(L2HConfig.getLegendaryThresholds(), diff);
                    if (l2fix$nonProtectedLegendaryCount >= maxAllowed) {
                        capped = 0;
                    } else {
                        l2fix$nonProtectedLegendaryCount++;
                    }
                }
            }
        }

        // === Refund budget for reduction ===
        if (capped < newRank) {
            int refund = (newRank - capped) * cost;
            level += refund;
        }

        // === Apply ===
        L2FIX$LOG.debug("[SetRank] FINAL trait={}, newRank={}, capped={}, refund={}, levelAfter={}",
                trait.getID(), newRank, capped, (capped < newRank ? (newRank - capped) * cost : 0),
                level);

        if (capped > 0) {
            traits.put(trait, capped);
        } else {
            traits.remove(trait);
        }
    }

    private void l2fix$ensureInit() {
        if (l2fix$legendaryCounted) return;

        l2fix$mobLevel = mobLevel;

        l2fix$protectedIds = l2fix$appliedPresetIds;

        l2fix$globalLevelCap = L2HConfig.getThreshold(
                L2HConfig.getLevelThresholds(), l2fix$mobLevel);
        l2fix$perTraitCaps = L2HConfig.getPerTraitThresholds();
        l2fix$extraLegendaryIds = L2HConfig.getExtraLegendaryIds();

        L2FIX$LOG.debug("[EnsureInit] mobLevel={}, globalLevelCap={}, protectedIds={}, extraLegendaryIds={}",
                l2fix$mobLevel, l2fix$globalLevelCap, l2fix$protectedIds, l2fix$extraLegendaryIds);

        if (traits != null && L2HConfig.COMMON.legendaryEnabled.get()) {
            for (Map.Entry<MobTrait, Integer> e : traits.entrySet()) {
                String id = e.getKey().getID();
                boolean isLegendary = e.getKey() instanceof LegendaryTrait || l2fix$extraLegendaryIds.contains(id);
                if (e.getValue() > 0 && isLegendary && !l2fix$protectedIds.contains(id)) {
                    l2fix$nonProtectedLegendaryCount++;
                }
            }
        }

        l2fix$legendaryCounted = true;
    }

    @Inject(method = "generate", at = @At("TAIL"))
    private void l2fix$afterGenerate(CallbackInfo ci) {
        if (entity == null || !entity.isAlive()) return;

        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (cap == null) return;

        if (cap.traits.isEmpty()) return;

        if (!entity.level().isClientSide()) {
            cap.syncToClient(entity);
        }
    }
}
