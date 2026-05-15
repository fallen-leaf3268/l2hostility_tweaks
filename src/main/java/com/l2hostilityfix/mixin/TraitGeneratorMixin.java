package com.l2hostilityfix.mixin;

import com.l2hostilityfix.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.L2Hostility;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;

@Mixin(value = TraitGenerator.class, remap = false)
public class TraitGeneratorMixin {

    private static final ResourceLocation NBT_CONDITION_ID = new ResourceLocation("l2hostilityfix", "nbt");

    private static Registry<MobTrait> TRAIT_REGISTRY;

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

            for (Object tb : presets) {
                String s = tb.toString();
                int start = s.indexOf("trait=");
                int end = s.indexOf(",", start);
                if (start < 0 || end <= start) continue;
                String traitId = s.substring(start + 6, end);
                MobTrait mt = traitReg.get(new ResourceLocation(traitId));
                if (mt == null) continue;

                int minLevel = 1;
                int minIdx = s.indexOf("min=");
                if (minIdx >= 0) {
                    int minEnd = s.indexOf(",", minIdx);
                    if (minEnd < 0) minEnd = s.indexOf("]", minIdx);
                    try { minLevel = Integer.parseInt(s.substring(minIdx + 4, minEnd)); } catch (NumberFormatException ignored) {}
                }

                mt.initialize(entity, minLevel);

                if (traits != null) {
                    Object existing = traits.get(mt);
                    int currentRank = existing instanceof Integer ? (Integer) existing : 0;
                    if (currentRank < minLevel) {
                        traits.put(mt, minLevel);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
