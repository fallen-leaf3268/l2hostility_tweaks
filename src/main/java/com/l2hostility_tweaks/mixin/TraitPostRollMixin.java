package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(value = TraitGenerator.class, remap = false)
public class TraitPostRollMixin {

    private static final Logger L2FIX$LOG = LoggerFactory.getLogger("L2HostilityFix/TraitGen");

    @Unique
    private boolean l2fix$legendaryCounted;

    @Unique
    private int l2fix$nonProtectedLegendaryCount;

    @Unique
    private Set<String> l2fix$protectedIds;

    @Unique
    private int l2fix$mobLevel;

    @Unique
    private int l2fix$globalLevelCap;

    @Unique
    private Map<String, int[]> l2fix$perTraitCaps;

    @Unique
    private Set<String> l2fix$extraLegendaryIds;

    // === 早期拦截：关闭生物等级 / 关闭全部词条 ===
    @ModifyVariable(method = "generateTraits", at = @At("HEAD"), argsOnly = true, index = 2)
    private static int l2fix$overrideMobLevel(int mobLevel) {
        if (L2HConfig.isDisableAllTraits()) {
            return 0;
        }
        return mobLevel;
    }

    // === 早期拦截：关闭非预设词条（保留 genBase 预设，跳过随机生成） ===
    @Inject(method = "generate", at = @At("HEAD"))
    private void l2fix$interceptGenerate(CallbackInfo ci) {
        if (L2HConfig.isDisableNonPresetTraits()) {
            TraitGenerator self = (TraitGenerator) (Object) this;
            TraitGenerationHelper.setLevel(self, 0);
        }
    }

    @Redirect(method = "generate",
            at = @At(value = "INVOKE",
                    target = "Ldev/xkmc/l2hostility/content/logic/MobDifficultyCollector;getMaxTraitLevel()I"))
    private int l2fix$overrideMaxTraitLevel(MobDifficultyCollector ins) {
        int original = ins.getMaxTraitLevel();
        if (!L2HConfig.COMMON.levelCapEnabled.get()) return original;

        TraitGenerator self = (TraitGenerator) (Object) this;
        int diff = TraitGenerationHelper.getMobLevel(self);
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
        if (newRank <= 0) return;

        HashMap<MobTrait, Integer> traits = TraitGenerationHelper.getTraits(self);
        if (traits == null) return;

        MobDifficultyCollector ins = TraitGenerationHelper.getIns(self);
        int cost = ins != null ? trait.getCost(ins.trait_cost) : 1;
        int capped = newRank;

        l2fix$ensureInit(self);

        L2FIX$LOG.debug("[SetRank] trait={}, newRank={}, mobLevel={}, globalLevelCap={}, protectedIds={}",
                trait.getID(), newRank, l2fix$mobLevel, l2fix$globalLevelCap, l2fix$protectedIds);

        // === Level cap ===
        if (L2HConfig.COMMON.levelCapEnabled.get() && capped > 1) {
            int diff = l2fix$mobLevel;
            if (diff < L2HConfig.COMMON.levelCapUnlimited.get()
                    && !l2fix$protectedIds.contains(trait.getID())) {
                int maxLv = l2fix$globalLevelCap;
                int[] custom = l2fix$perTraitCaps.get(trait.getID());
                if (custom != null) {
                    int idx = capped - 2;
                    if (idx >= 0 && idx < custom.length && diff < custom[idx]) {
                        int allowed = 0;
                        for (int val : custom) {
                            if (diff >= val) allowed++;
                            else break;
                        }
                        maxLv = Math.min(maxLv, allowed + 1);
                    }
                }
                if (capped > maxLv) capped = maxLv;
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
            TraitGenerationHelper.setLevel(self, TraitGenerationHelper.getLevel(self) + refund);
        }

        // === Apply ===
        L2FIX$LOG.debug("[SetRank] FINAL trait={}, newRank={}, capped={}, refund={}, levelAfter={}",
                trait.getID(), newRank, capped, (capped < newRank ? (newRank - capped) * cost : 0),
                TraitGenerationHelper.getLevel(self));

        if (capped > 0) {
            traits.put(trait, capped);
        } else {
            traits.remove(trait);
        }
    }

    private void l2fix$ensureInit(TraitGenerator self) {
        if (l2fix$legendaryCounted) return;

        l2fix$mobLevel = TraitGenerationHelper.getMobLevel(self);

        LivingEntity entity = TraitGenerationHelper.getEntity(self);
        l2fix$protectedIds = entity != null
                ? TraitGenerationHelper.getDataPackPresetIds(entity)
                : java.util.Collections.emptySet();

        l2fix$globalLevelCap = L2HConfig.getThreshold(
                L2HConfig.getLevelThresholds(), l2fix$mobLevel);
        l2fix$perTraitCaps = L2HConfig.getPerTraitThresholds();
        l2fix$extraLegendaryIds = L2HConfig.getExtraLegendaryIds();

        L2FIX$LOG.debug("[EnsureInit] mobLevel={}, globalLevelCap={}, protectedIds={}, extraLegendaryIds={}",
                l2fix$mobLevel, l2fix$globalLevelCap, l2fix$protectedIds, l2fix$extraLegendaryIds);

        HashMap<MobTrait, Integer> traits = TraitGenerationHelper.getTraits(self);
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
        TraitGenerator self = (TraitGenerator) (Object) this;
        LivingEntity entity = TraitGenerationHelper.getEntity(self);
        if (entity == null || !entity.isAlive()) return;

        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (cap == null) return;

        if (cap.traits.isEmpty()) return;

        if (!entity.level().isClientSide()) {
            cap.syncToClient(entity);
        }
    }
}
