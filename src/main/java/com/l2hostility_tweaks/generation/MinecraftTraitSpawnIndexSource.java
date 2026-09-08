package com.l2hostility_tweaks.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.ConfigInput;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.EntityInput;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.Inputs;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.PresetInput;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.Settings;
import com.l2hostility_tweaks.generation.TraitSpawnIndexBuilder.TraitInput;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.EntityConfigView;
import com.l2hostility_tweaks.util.EntityConfigDisplayData;
import com.l2hostility_tweaks.util.EntityConfigNbtData;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.config.TraitConfig;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.L2Hostility;
import dev.xkmc.l2hostility.init.data.LHTagGen;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MinecraftTraitSpawnIndexSource {

    private static final Comparator<ResourceLocation> ID_ORDER = Comparator.comparing(ResourceLocation::toString);

    public static Inputs capture(MinecraftServer server) {
        Objects.requireNonNull(server);
        ConfigGroups configs = captureConfigs();
        List<EntityInput> entities = captureEntities(server, configs);
        List<TraitInput> traits = captureTraits();
        return new Inputs(entities, traits, captureSettings());
    }

    private static ConfigGroups captureConfigs() {
        Map<ResourceLocation, List<ConfigInput>> base = new HashMap<>();
        Map<ResourceLocation, List<ConfigInput>> conditional = new HashMap<>();
        EntityConfig merged = L2Hostility.ENTITY.getMerged();
        for (EntityConfig.Config config : merged.list) {
            EntityConfigNbtData.State nbtState = config instanceof EntityConfigNbtData data
                    ? data.l2fix$getNbtConditionState() : EntityConfigNbtData.State.NONE;
            if (!shouldIncludeConfig(nbtState)) continue;
            ConfigInput input = configInput(config);
            Map<ResourceLocation, List<ConfigInput>> destination = isConditional(config)
                    ? conditional : base;
            for (EntityType<?> type : config.entities) {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (entityId != null) {
                    destination.computeIfAbsent(entityId, ignored -> new ArrayList<>()).add(input);
                }
            }
        }
        return new ConfigGroups(base, conditional);
    }

    static boolean shouldIncludeConfig(EntityConfigNbtData.State nbtState) {
        return nbtState != EntityConfigNbtData.State.INVALID;
    }

    private static ConfigInput configInput(EntityConfig.Config config) {
        EntityConfigDisplayData display = config instanceof EntityConfigDisplayData data ? data : null;
        ResourceLocation sourceId = display == null ? null : display.l2fix$getSourceId();
        String conditionJson = conditionJson(config, display == null ? null : display.l2fix$getRawConfig());
        var difficulty = config.difficulty();
        EntityConfigView view = new EntityConfigView(
                sourceId, conditionJson, difficulty.min(), difficulty.base(), difficulty.variation(),
                difficulty.scale(), difficulty.apply_chance(), difficulty.trait_chance(),
                difficulty.suppression(), config.minSpawnLevel,
                effectiveMaxLevel(config.maxLevel, LHConfig.COMMON.maxMobLevel.get()),
                config.maxTraitCount, presetTraitsOnly(config));
        Set<ResourceLocation> blacklist = new LinkedHashSet<>();
        for (MobTrait trait : config.blacklist()) {
            ResourceLocation id = LHTraits.TRAITS.get().getKey(trait);
            if (id != null) blacklist.add(id);
        }
        List<PresetInput> presets = new ArrayList<>();
        for (EntityConfig.TraitBase preset : config.traits()) {
            MobTrait trait = preset.trait();
            if (trait == null) continue;
            ResourceLocation traitId = LHTraits.TRAITS.get().getKey(trait);
            if (traitId == null) continue;
            EntityConfig.TraitCondition condition = preset.condition();
            presets.add(new PresetInput(
                    traitId, preset.free(), preset.min(), presetCap(preset),
                    condition == null ? 1.0 : condition.chance(),
                    condition == null ? 0 : condition.lv(),
                    condition == null ? null : condition.id()));
        }
        return new ConfigInput(sourceId, conditionJson, view, blacklist, presets);
    }

    private static boolean isConditional(EntityConfig.Config config) {
        EntityConfigDisplayData display = config instanceof EntityConfigDisplayData data ? data : null;
        JsonObject raw = display == null ? null : display.l2fix$getRawConfig();
        JsonElement special = raw == null ? null : raw.get("specialConditions");
        boolean hasSpecial = special != null && !special.isJsonNull()
                && (!special.isJsonArray() || special.getAsJsonArray().size() > 0);
        EntityConfigNbtData.State nbtState = config instanceof EntityConfigNbtData data
                ? data.l2fix$getNbtConditionState() : EntityConfigNbtData.State.NONE;
        return hasSpecial || nbtState == EntityConfigNbtData.State.VALID;
    }

    private static String conditionJson(EntityConfig.Config config, JsonObject raw) {
        if (raw == null) return "";
        JsonObject conditions = new JsonObject();
        JsonElement special = raw.get("specialConditions");
        if (special != null && !special.isJsonNull()
                && (!special.isJsonArray() || special.getAsJsonArray().size() > 0)) {
            conditions.add("specialConditions", special.deepCopy());
        }
        if (config instanceof EntityConfigNbtData data
                && data.l2fix$getNbtConditionState() == EntityConfigNbtData.State.VALID) {
            JsonElement nbt = raw.get("nbt");
            if (nbt != null && nbt.isJsonObject()) conditions.add("nbt", nbt.deepCopy());
        }
        return conditions.size() == 0 ? "" : conditions.toString();
    }

    private static List<EntityInput> captureEntities(MinecraftServer server, ConfigGroups configs) {
        var registered = ForgeRegistries.ENTITY_TYPES
                .getEntries().stream()
                .map(entry -> Map.entry(entry.getKey().location(), entry.getValue()))
                .sorted(Map.Entry.comparingByKey(ID_ORDER))
                .toList();
        List<EntityInput> entities = new ArrayList<>();
        for (var entry : registered) {
            Entity temporary;
            try {
                temporary = entry.getValue().create(server.overworld());
            } catch (RuntimeException exception) {
                L2Hostility.LOGGER.warn("Unable to inspect entity type {} for trait index", entry.getKey(), exception);
                continue;
            }
            if (temporary == null) continue;
            try {
                if (!(temporary instanceof LivingEntity living)) continue;
                boolean player = living instanceof Player;
                boolean traitCapabilityApplies = !player && MobTraitCap.HOLDER.isProper(living);
                if (!shouldIncludeEntity(true, player, traitCapabilityApplies)) continue;
                entities.add(new EntityInput(
                        entry.getKey(), entry.getValue().is(LHTagGen.NO_TRAIT),
                        configs.base().getOrDefault(entry.getKey(), List.of()),
                        configs.conditional().getOrDefault(entry.getKey(), List.of())));
            } finally {
                temporary.discard();
            }
        }
        return List.copyOf(entities);
    }

    static boolean shouldIncludeEntity(boolean living, boolean player, boolean traitCapabilityApplies) {
        return living && !player && traitCapabilityApplies;
    }

    static int effectiveMaxLevel(int entityMaxLevel, int globalMaxLevel) {
        return entityMaxLevel > 0 ? Math.min(entityMaxLevel, globalMaxLevel) : globalMaxLevel;
    }

    private static List<TraitInput> captureTraits() {
        return LHTraits.TRAITS.get().getValues().stream()
                .map(MinecraftTraitSpawnIndexSource::traitInput)
                .sorted(Comparator.comparing(TraitInput::traitId, ID_ORDER))
                .toList();
    }

    private static TraitInput traitInput(MobTrait trait) {
        ResourceLocation traitId = Objects.requireNonNull(LHTraits.TRAITS.get().getKey(trait));
        ResourceLocation itemId = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(trait.asItem()));
        TraitConfig config = trait.getConfig();
        return new TraitInput(
                traitId, itemId, config.weight, config.min_level, config.cost, config.max_rank,
                trait.isBanned(), tagEntries(config.getBlacklistTag()), tagEntries(config.getWhitelistTag()),
                overridesRuntimeAllow(trait));
    }

    private static Set<ResourceLocation> tagEntries(net.minecraft.tags.TagKey<EntityType<?>> tag) {
        var tags = ForgeRegistries.ENTITY_TYPES.tags();
        if (tags == null) return Set.of();
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        tags.getTag(tag).stream()
                .map(ForgeRegistries.ENTITY_TYPES::getKey)
                .filter(Objects::nonNull)
                .sorted(ID_ORDER)
                .forEach(ids::add);
        return Set.copyOf(ids);
    }

    private static boolean overridesRuntimeAllow(MobTrait trait) {
        try {
            Method method = trait.getClass().getMethod(
                    "allow", LivingEntity.class, int.class, int.class);
            return method.getDeclaringClass() != MobTrait.class;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing MobTrait.allow runtime gate", exception);
        }
    }

    private static boolean presetTraitsOnly(EntityConfig.Config config) {
        try {
            return config.getClass().getField("presetTraitsOnly").getBoolean(config);
        } catch (NoSuchFieldException exception) {
            return false;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to read parsed presetTraitsOnly", exception);
        }
    }

    private static boolean presetCap(EntityConfig.TraitBase preset) {
        try {
            return (boolean) preset.getClass().getMethod("cap").invoke(preset);
        } catch (NoSuchMethodException exception) {
            return false;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read parsed preset cap", exception);
        }
    }

    private static Settings captureSettings() {
        List<List<ResourceLocation>> exclusions = new ArrayList<>();
        if (L2HConfig.isExclusionEnabled()) {
            for (L2HConfig.ExclusionGroup group : L2HConfig.getExclusionGroups()) {
                List<ResourceLocation> ids = group.traitIds().stream()
                        .map(ResourceLocation::tryParse)
                        .filter(Objects::nonNull)
                        .sorted(ID_ORDER)
                        .toList();
                if (!ids.isEmpty()) exclusions.add(ids);
            }
        }
        return new Settings(
                L2HConfig.isDisableNonPresetTraits(), L2HConfig.isDisableAllTraits(),
                L2HConfig.isDisableMobLevel(), L2HConfig.COMMON.legendaryEnabled.get(),
                L2HConfig.COMMON.levelCapEnabled.get(), exclusions);
    }

    private record ConfigGroups(Map<ResourceLocation, List<ConfigInput>> base,
                                Map<ResourceLocation, List<ConfigInput>> conditional) {}

    private MinecraftTraitSpawnIndexSource() {}
}
