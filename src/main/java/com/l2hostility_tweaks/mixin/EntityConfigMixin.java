package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.condition.NbtCondition;
import com.l2hostility_tweaks.util.EntityConfigNbtData;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = EntityConfig.class, remap = false)
public class EntityConfigMixin {

    @Shadow
    @Final
    private Map<EntityType<?>, EntityConfig.Config> cache;

    @Shadow
    @Final
    private Map<ResourceLocation, ArrayList<Pair<SpecialConfigCondition<?>, EntityConfig.Config>>> conditions;

    @Shadow
    @Final
    public ArrayList<EntityConfig.Config> list;

    private static final ResourceLocation NBT_CONDITION_ID = new ResourceLocation("l2hostility_tweaks", "nbt");

    enum Decision {
        ORDINARY,
        CONDITIONAL,
        DISABLED
    }

    @Unique
    static Decision l2fix$decision(EntityConfigNbtData.State state) {
        return switch (state) {
            case NONE -> Decision.ORDINARY;
            case VALID -> Decision.CONDITIONAL;
            case INVALID -> Decision.DISABLED;
        };
    }

    @Inject(method = "postMerge", at = @At("TAIL"))
    private void l2fix$onPostMerge(CallbackInfo ci) {
        // Collect non-NBT defaults per entity type (for restoration)
        Map<EntityType<?>, EntityConfig.Config> defaultConfigs = new LinkedHashMap<>();
        for (EntityConfig.Config config : list) {
            EntityConfigNbtData data = (EntityConfigNbtData) (Object) config;
            if (l2fix$decision(data.l2fix$getNbtConditionState()) == Decision.ORDINARY) {
                for (EntityType<?> type : config.entities) {
                    defaultConfigs.put(type, config);
                }
            }
        }

        // Register NBT configs and remove from simple cache
        for (EntityConfig.Config config : list) {
            EntityConfigNbtData data = (EntityConfigNbtData) (Object) config;
            Decision decision = l2fix$decision(data.l2fix$getNbtConditionState());
            if (decision == Decision.ORDINARY) continue;

            if (decision == Decision.CONDITIONAL) {
                NbtCondition nbtCondition = new NbtCondition(data.l2fix$getNbtCondition());
                conditions.computeIfAbsent(NBT_CONDITION_ID, k -> new ArrayList<>())
                        .add(Pair.of(nbtCondition, config));
            }

            for (EntityType<?> type : config.entities) {
                if (cache.get(type) == config) {
                    cache.remove(type);
                }
            }
        }

        // Restore non-NBT configs displaced by NBT overwrites
        for (Map.Entry<EntityType<?>, EntityConfig.Config> entry : defaultConfigs.entrySet()) {
            if (!cache.containsKey(entry.getKey())) {
                cache.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
