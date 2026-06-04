package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.condition.NbtCondition;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import dev.xkmc.l2hostility.init.L2Hostility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.*;

@Mixin(value = EntityConfig.class, remap = false)
public class EntityConfigMixin {

    private static final Logger LOG = LoggerFactory.getLogger("L2HostilityFix/EntityConfig");

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
    private static volatile Field NBT_FIELD;
    private static volatile boolean NBT_FIELD_LOOKED_UP;

    private static Map<String, Object> l2fix$getNbt(EntityConfig.Config config) {
        if (!NBT_FIELD_LOOKED_UP) {
            try {
                NBT_FIELD = EntityConfig.Config.class.getField("nbt");
            } catch (NoSuchFieldException e) {
                LOG.error("[EntityConfigMixin] Failed to find 'nbt' field on Config class! NBT conditions will not work.", e);
            }
            NBT_FIELD_LOOKED_UP = true;
        }
        if (NBT_FIELD == null) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) NBT_FIELD.get(config);
            return result;
        } catch (IllegalAccessException e) {
            LOG.error("[EntityConfigMixin] Failed to access 'nbt' field", e);
            return null;
        }
    }

    @Inject(method = "postMerge", at = @At("TAIL"))
    private void l2fix$onPostMerge(CallbackInfo ci) {
        // Collect non-NBT defaults per entity type (for restoration)
        Map<EntityType<?>, EntityConfig.Config> defaultConfigs = new LinkedHashMap<>();
        for (EntityConfig.Config config : list) {
            Map<String, Object> nbt = l2fix$getNbt(config);
            if (nbt == null || nbt.isEmpty()) {
                for (EntityType<?> type : config.entities) {
                    defaultConfigs.put(type, config);
                }
            }
        }

        // Register NBT configs and remove from simple cache
        for (EntityConfig.Config config : list) {
            Map<String, Object> nbt = l2fix$getNbt(config);
            if (nbt == null || nbt.isEmpty()) continue;

            NbtCondition nbtCondition = new NbtCondition(nbt);
            conditions.computeIfAbsent(NBT_CONDITION_ID, k -> new ArrayList<>())
                    .add(Pair.of(nbtCondition, config));

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
