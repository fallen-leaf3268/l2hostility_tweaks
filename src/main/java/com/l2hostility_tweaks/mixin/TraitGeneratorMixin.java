package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import dev.xkmc.l2library.base.L2Registrate;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Shadow
    private void genBase(EntityConfig.TraitBase preset) {
        throw new AssertionError();
    }

    @Redirect(method = "<init>", at = @At(
            value = "FIELD",
            target = "Ldev/xkmc/l2hostility/init/registrate/LHTraits;TRAITS:Ldev/xkmc/l2library/base/L2Registrate$RegistryInstance;",
            opcode = Opcodes.GETSTATIC), require = 1)
    private L2Registrate.RegistryInstance<MobTrait> l2fix$applyNbtPresetsBeforePool() {
        for (EntityConfig.TraitBase preset :
                TraitGenerationHelper.selectActiveNbtPresets(entity, mobLevel, ins)) {
            genBase(preset);
        }
        return LHTraits.TRAITS;
    }

    @Inject(method = "generate", at = @At(
            value = "INVOKE",
            target = "Ljava/util/HashMap;entrySet()Ljava/util/Set;",
            shift = At.Shift.BEFORE), require = 1)
    private void l2fix$prepareFinalTraits(CallbackInfo ci) {
        TraitGenerationHelper.applyFinalFilters(
                entity, traits, mobLevel,
                ((TraitGenerationHelper.PresetState) (Object) this)
                        .l2fix$getAppliedPresetIds());
    }

}
