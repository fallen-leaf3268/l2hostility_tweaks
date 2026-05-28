package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.L2Hostility;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

@Mixin(value = TraitGenerator.class, remap = false)
public class TraitGeneratorMixin {

    private static final Logger LOG = LoggerFactory.getLogger("L2HostilityFix/NbtPresetGen");
    private static final ResourceLocation NBT_CONDITION_ID = new ResourceLocation("l2hostility_tweaks", "nbt");

    private static Registry<MobTrait> TRAIT_REGISTRY;
    private static Field traitField;
    private static Field minField;
    private static boolean fieldsResolved;

    @SuppressWarnings("unchecked")
    private static Registry<MobTrait> getTraitRegistry() {
        if (TRAIT_REGISTRY == null) {
            for (String key : new String[]{"l2hostility:trait", "l2hostility:mob_trait", "l2hostility:traits"}) {
                Registry<?> reg = BuiltInRegistries.REGISTRY.get(new ResourceLocation(key));
                if (reg != null) {
                    TRAIT_REGISTRY = (Registry<MobTrait>) reg;
                    break;
                }
            }
        }
        return TRAIT_REGISTRY;
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "generate", at = @At("TAIL"))
    private void afterGenerate(CallbackInfo ci) {
        try {
            TraitGenerator self = (TraitGenerator) (Object) this;
            LivingEntity entity = TraitGenerationHelper.getEntity(self);
            if (entity == null) return;

            EntityConfig merged = (EntityConfig) L2Hostility.ENTITY.getMerged();
            EntityConfig.Config nbtConfig = merged.get(entity.getType(), NBT_CONDITION_ID, LivingEntity.class, entity);
            if (nbtConfig == null) return;

            ConfigAccessor acc = (ConfigAccessor) (Object) nbtConfig;
            ArrayList<?> presets = acc.getTraitsList();
            if (presets == null || presets.isEmpty()) return;

            Registry<MobTrait> traitReg = getTraitRegistry();
            if (traitReg == null) return;

            HashMap<Object, Object> traits = (HashMap<Object, Object>) (Object) TraitGenerationHelper.getTraits(self);

            int appliedCount = 0;
            for (Object tb : presets) {
                if (!resolveFields(tb)) continue;

                String traitId;
                int minLevel;
                try {
                    Object traitVal = traitField.get(tb);
                    traitId = traitVal instanceof ResourceLocation ? traitVal.toString() : String.valueOf(traitVal);
                    minLevel = minField.getInt(tb);
                } catch (Exception e) {
                    LOG.warn("[NbtPresetGen] Reflection failed for {}", tb.getClass().getName());
                    continue;
                }

                MobTrait mt = traitReg.get(new ResourceLocation(traitId));
                if (mt == null) {
                    LOG.warn("[NbtPresetGen] Trait '{}' not found in registry, skipping", traitId);
                    continue;
                }

                mt.initialize(entity, minLevel);

                if (traits != null) {
                    Object existing = traits.get(mt);
                    int currentRank = existing instanceof Integer ? (Integer) existing : 0;
                    if (currentRank < minLevel) {
                        traits.put(mt, minLevel);
                    }
                }
                appliedCount++;
            }
            if (appliedCount > 0) {
                LOG.debug("[NbtPresetGen] Applied {} NBT preset traits to {}", appliedCount, entity.getName().getString());
            }
        } catch (Exception e) {
            LOG.error("[NbtPresetGen] Failed to apply NBT presets", e);
        }
    }

    private static boolean resolveFields(Object tb) {
        if (fieldsResolved) return traitField != null;
        Class<?> clazz = tb.getClass();
        try {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                String name = f.getName();
                if (name.equals("trait") || name.equals("id") || name.equals("traitId")) {
                    traitField = f;
                } else if (name.equals("min") || name.equals("minLevel") || name.equals("min_rank")) {
                    minField = f;
                }
            }
            fieldsResolved = true;
            if (traitField == null || minField == null) {
                LOG.error("[NbtPresetGen] Could not find trait/min fields on {}", clazz.getName());
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.error("[NbtPresetGen] Failed to resolve fields on {}", clazz.getName(), e);
            fieldsResolved = true;
            return false;
        }
    }
}
