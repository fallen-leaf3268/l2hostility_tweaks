package com.l2hostility_tweaks.mixin;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(value = MobTraitCap.class, remap = false)
public interface MobTraitPendingAccessor {

    @Accessor("pending")
    ArrayList<Pair<MobTrait, Integer>> l2fix$getPendingTraits();
}
