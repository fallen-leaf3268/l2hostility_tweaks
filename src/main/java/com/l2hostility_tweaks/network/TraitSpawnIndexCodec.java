package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.generation.view.TraitBlockReason;
import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TraitSpawnIndexCodec {

    public static final int MAX_MOBS = 4096;
    public static final int MAX_TRAITS_PER_MOB = 2048;
    public static final int MAX_CONFIGS_PER_MOB = 256;
    public static final int MAX_CONSTRAINTS = 256;
    public static final int MAX_STRING_LENGTH = 32767;
    public static final int MAX_TOTAL_ENTRIES = 1_000_000;

    private TraitSpawnIndexCodec() {
    }

    public static CompoundTag encode(TraitSpawnIndexSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Counter counter = new Counter();
        CompoundTag root = new CompoundTag();
        root.putLong("revision", snapshot.revision());
        validateNonnegative(snapshot.warningCount(), "warningCount");
        root.putInt("warningCount", snapshot.warningCount());
        root.put("mobs", writeMobs(snapshot.mobs(), counter));
        root.putInt("mobCount", snapshot.mobs().size());
        return root;
    }

    public static TraitSpawnIndexSnapshot decode(CompoundTag root) {
        Objects.requireNonNull(root, "root");
        long revision = requiredLong(root, "revision");
        int warningCount = requiredNonnegative(root, "warningCount");
        Counter counter = new Counter();
        ListTag mobTags = checkedList(root, "mobs", "mobCount", MAX_MOBS, counter, Tag.TAG_COMPOUND);
        List<TraitSpawnIndexSnapshot.MobTraitOverview> mobs = new ArrayList<>(mobTags.size());
        for (int i = 0; i < mobTags.size(); i++) {
            mobs.add(readMob(requiredCompound(mobTags, i, "mobs"), counter));
        }
        return new TraitSpawnIndexSnapshot(revision, mobs, warningCount);
    }

    public static void validateProbability(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Probability must be finite and between 0 and 1: " + value);
        }
    }

    public static ResourceLocation parseId(String value) {
        validateString(value, "resource id");
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) throw new IllegalArgumentException("Invalid resource id: " + value);
        return parsed;
    }

    private static ListTag writeMobs(List<TraitSpawnIndexSnapshot.MobTraitOverview> mobs, Counter counter) {
        checkSize(mobs, MAX_MOBS, "mobs", counter);
        ListTag tags = new ListTag();
        for (TraitSpawnIndexSnapshot.MobTraitOverview mob : mobs) {
            CompoundTag tag = new CompoundTag();
            tag.putString("entityId", idString(mob.entityId(), "entityId"));
            putNonnegative(tag, "variantIndex", mob.variantIndex());
            tag.put("configs", writeConfigs(mob.configs(), counter));
            tag.putInt("configCount", mob.configs().size());
            tag.put("presets", writePresets(mob.presets(), counter));
            tag.putInt("presetCount", mob.presets().size());
            tag.put("pool", writePool(mob.pool(), counter));
            tag.putInt("poolCount", mob.pool().size());
            tag.put("blocked", writeBlocked(mob.blocked(), counter));
            tag.putInt("blockedCount", mob.blocked().size());
            tag.put("dynamicConstraints", writeConstraints(mob.dynamicConstraints(), counter));
            tag.putInt("constraintCount", mob.dynamicConstraints().size());
            tags.add(tag);
        }
        return tags;
    }

    private static TraitSpawnIndexSnapshot.MobTraitOverview readMob(CompoundTag tag, Counter counter) {
        ResourceLocation entityId = parseId(requiredString(tag, "entityId"));
        int variantIndex = requiredNonnegative(tag, "variantIndex");
        List<TraitSpawnIndexSnapshot.EntityConfigView> configs = readConfigs(
                checkedList(tag, "configs", "configCount", MAX_CONFIGS_PER_MOB, counter, Tag.TAG_COMPOUND), counter);
        List<TraitSpawnIndexSnapshot.PresetTraitView> presets = readPresets(
                checkedList(tag, "presets", "presetCount", MAX_TRAITS_PER_MOB, counter, Tag.TAG_COMPOUND), counter);
        List<TraitSpawnIndexSnapshot.PoolTraitView> pool = readPool(
                checkedList(tag, "pool", "poolCount", MAX_TRAITS_PER_MOB, counter, Tag.TAG_COMPOUND), counter);
        List<TraitSpawnIndexSnapshot.BlockedTraitView> blocked = readBlocked(
                checkedList(tag, "blocked", "blockedCount", MAX_TRAITS_PER_MOB, counter, Tag.TAG_COMPOUND), counter);
        List<TraitDynamicConstraint> constraints = readConstraints(
                checkedList(tag, "dynamicConstraints", "constraintCount", MAX_CONSTRAINTS, counter, Tag.TAG_COMPOUND), counter);
        return new TraitSpawnIndexSnapshot.MobTraitOverview(
                entityId, variantIndex, configs, presets, pool, blocked, constraints);
    }

    private static ListTag writeConfigs(List<TraitSpawnIndexSnapshot.EntityConfigView> configs, Counter counter) {
        checkSize(configs, MAX_CONFIGS_PER_MOB, "configs", counter);
        ListTag tags = new ListTag();
        for (TraitSpawnIndexSnapshot.EntityConfigView config : configs) {
            CompoundTag tag = new CompoundTag();
            putOptionalId(tag, "sourceId", config.sourceId());
            tag.putString("conditionJson", checkedString(config.conditionJson(), "conditionJson"));
            putNonnegative(tag, "minDifficulty", config.minDifficulty());
            putNonnegative(tag, "baseDifficulty", config.baseDifficulty());
            putFinite(tag, "variation", config.variation());
            putFinite(tag, "scale", config.scale());
            putProbability(tag, "applyChance", config.applyChance());
            putProbability(tag, "traitChance", config.traitChance());
            putProbability(tag, "suppression", config.suppression());
            putNonnegative(tag, "minSpawnLevel", config.minSpawnLevel());
            putNonnegative(tag, "maxLevel", config.maxLevel());
            putMaxTraitCount(tag, config.maxTraitCount());
            tag.putBoolean("presetTraitsOnly", config.presetTraitsOnly());
            tags.add(tag);
        }
        return tags;
    }

    private static List<TraitSpawnIndexSnapshot.EntityConfigView> readConfigs(ListTag tags, Counter counter) {
        List<TraitSpawnIndexSnapshot.EntityConfigView> configs = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = requiredCompound(tags, i, "configs");
            configs.add(new TraitSpawnIndexSnapshot.EntityConfigView(
                    optionalId(tag, "sourceId"), requiredString(tag, "conditionJson"),
                    requiredNonnegative(tag, "minDifficulty"), requiredNonnegative(tag, "baseDifficulty"),
                    requiredFinite(tag, "variation"), requiredFinite(tag, "scale"),
                    requiredProbability(tag, "applyChance"), requiredProbability(tag, "traitChance"),
                    requiredProbability(tag, "suppression"), requiredNonnegative(tag, "minSpawnLevel"),
                    requiredNonnegative(tag, "maxLevel"), requiredMaxTraitCount(tag),
                    requiredBoolean(tag, "presetTraitsOnly")));
        }
        return configs;
    }

    private static ListTag writePresets(List<TraitSpawnIndexSnapshot.PresetTraitView> presets, Counter counter) {
        checkSize(presets, MAX_TRAITS_PER_MOB, "presets", counter);
        ListTag tags = new ListTag();
        for (TraitSpawnIndexSnapshot.PresetTraitView preset : presets) {
            CompoundTag tag = new CompoundTag();
            tag.putString("traitId", idString(preset.traitId(), "traitId"));
            tag.putString("itemId", idString(preset.itemId(), "itemId"));
            putNonnegative(tag, "freeRank", preset.freeRank());
            putNonnegative(tag, "minRank", preset.minRank());
            tag.putBoolean("cap", preset.cap());
            putProbability(tag, "chance", preset.chance());
            putNonnegative(tag, "conditionLevel", preset.conditionLevel());
            putOptionalId(tag, "advancementId", preset.advancementId());
            putOptionalId(tag, "sourceId", preset.sourceId());
            tag.putString("conditionJson", checkedString(preset.conditionJson(), "conditionJson"));
            tag.put("dynamicConstraints", writeConstraints(preset.dynamicConstraints(), counter));
            tag.putInt("constraintCount", preset.dynamicConstraints().size());
            tags.add(tag);
        }
        return tags;
    }

    private static List<TraitSpawnIndexSnapshot.PresetTraitView> readPresets(ListTag tags, Counter counter) {
        List<TraitSpawnIndexSnapshot.PresetTraitView> presets = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = requiredCompound(tags, i, "presets");
            List<TraitDynamicConstraint> constraints = readConstraints(
                    checkedList(tag, "dynamicConstraints", "constraintCount", MAX_CONSTRAINTS, counter, Tag.TAG_COMPOUND), counter);
            presets.add(new TraitSpawnIndexSnapshot.PresetTraitView(
                    parseId(requiredString(tag, "traitId")), parseId(requiredString(tag, "itemId")),
                    requiredNonnegative(tag, "freeRank"), requiredNonnegative(tag, "minRank"),
                    requiredBoolean(tag, "cap"), requiredProbability(tag, "chance"),
                    requiredNonnegative(tag, "conditionLevel"), optionalId(tag, "advancementId"), optionalId(tag, "sourceId"),
                    requiredString(tag, "conditionJson"), constraints));
        }
        return presets;
    }

    private static ListTag writePool(List<TraitSpawnIndexSnapshot.PoolTraitView> pool, Counter counter) {
        checkSize(pool, MAX_TRAITS_PER_MOB, "pool", counter);
        ListTag tags = new ListTag();
        for (TraitSpawnIndexSnapshot.PoolTraitView entry : pool) {
            CompoundTag tag = new CompoundTag();
            tag.putString("traitId", idString(entry.traitId(), "traitId"));
            tag.putString("itemId", idString(entry.itemId(), "itemId"));
            putNonnegative(tag, "weight", entry.weight());
            putNonnegative(tag, "minLevel", entry.minLevel());
            putNonnegative(tag, "cost", entry.cost());
            putNonnegative(tag, "maxRank", entry.maxRank());
            tags.add(tag);
        }
        return tags;
    }

    private static List<TraitSpawnIndexSnapshot.PoolTraitView> readPool(ListTag tags, Counter counter) {
        List<TraitSpawnIndexSnapshot.PoolTraitView> pool = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = requiredCompound(tags, i, "pool");
            pool.add(new TraitSpawnIndexSnapshot.PoolTraitView(
                    parseId(requiredString(tag, "traitId")), parseId(requiredString(tag, "itemId")),
                    requiredNonnegative(tag, "weight"), requiredNonnegative(tag, "minLevel"),
                    requiredNonnegative(tag, "cost"), requiredNonnegative(tag, "maxRank")));
        }
        return pool;
    }

    private static ListTag writeBlocked(List<TraitSpawnIndexSnapshot.BlockedTraitView> blocked, Counter counter) {
        checkSize(blocked, MAX_TRAITS_PER_MOB, "blocked", counter);
        ListTag tags = new ListTag();
        for (TraitSpawnIndexSnapshot.BlockedTraitView entry : blocked) {
            CompoundTag tag = new CompoundTag();
            tag.putString("traitId", idString(entry.traitId(), "traitId"));
            tag.putString("itemId", idString(entry.itemId(), "itemId"));
            tag.put("contexts", writeContexts(entry.contexts(), counter));
            tag.putInt("contextCount", entry.contexts().size());
            tags.add(tag);
        }
        return tags;
    }

    private static List<TraitSpawnIndexSnapshot.BlockedTraitView> readBlocked(ListTag tags, Counter counter) {
        List<TraitSpawnIndexSnapshot.BlockedTraitView> blocked = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = requiredCompound(tags, i, "blocked");
            List<TraitSpawnIndexSnapshot.BlockedContextView> contexts = readContexts(
                    checkedList(tag, "contexts", "contextCount", MAX_CONSTRAINTS, counter, Tag.TAG_COMPOUND), counter);
            blocked.add(new TraitSpawnIndexSnapshot.BlockedTraitView(
                    parseId(requiredString(tag, "traitId")), parseId(requiredString(tag, "itemId")), contexts));
        }
        return blocked;
    }

    private static ListTag writeContexts(List<TraitSpawnIndexSnapshot.BlockedContextView> contexts, Counter counter) {
        checkSize(contexts, MAX_CONSTRAINTS, "contexts", counter);
        ListTag tags = new ListTag();
        for (TraitSpawnIndexSnapshot.BlockedContextView context : contexts) {
            CompoundTag tag = new CompoundTag();
            putOptionalId(tag, "sourceId", context.sourceId());
            checkSize(context.reasons(), MAX_CONSTRAINTS, "reasons", counter);
            ListTag reasons = new ListTag();
            for (TraitBlockReason reason : context.reasons()) {
                reasons.add(StringTag.valueOf(checkedString(reason.name(), "reason")));
            }
            tag.put("reasons", reasons);
            tag.putInt("reasonCount", context.reasons().size());
            tags.add(tag);
        }
        return tags;
    }

    private static List<TraitSpawnIndexSnapshot.BlockedContextView> readContexts(ListTag tags, Counter counter) {
        List<TraitSpawnIndexSnapshot.BlockedContextView> contexts = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = requiredCompound(tags, i, "contexts");
            ListTag reasonTags = checkedList(tag, "reasons", "reasonCount", MAX_CONSTRAINTS, counter, Tag.TAG_STRING);
            List<TraitBlockReason> reasons = new ArrayList<>(reasonTags.size());
            for (int reasonIndex = 0; reasonIndex < reasonTags.size(); reasonIndex++) {
                try {
                    reasons.add(TraitBlockReason.valueOf(requiredString(reasonTags, reasonIndex, "reasons")));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Invalid blocked reason", exception);
                }
            }
            contexts.add(new TraitSpawnIndexSnapshot.BlockedContextView(optionalId(tag, "sourceId"), reasons));
        }
        return contexts;
    }

    private static ListTag writeConstraints(List<TraitDynamicConstraint> constraints, Counter counter) {
        checkSize(constraints, MAX_CONSTRAINTS, "dynamicConstraints", counter);
        ListTag tags = new ListTag();
        for (TraitDynamicConstraint constraint : constraints) {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", checkedString(constraint.type(), "constraint type"));
            checkSize(constraint.arguments(), MAX_CONSTRAINTS, "constraint arguments", counter);
            ListTag arguments = new ListTag();
            for (String argument : constraint.arguments()) {
                arguments.add(StringTag.valueOf(checkedString(argument, "constraint argument")));
            }
            tag.put("arguments", arguments);
            tag.putInt("argumentCount", constraint.arguments().size());
            tags.add(tag);
        }
        return tags;
    }

    private static List<TraitDynamicConstraint> readConstraints(ListTag tags, Counter counter) {
        List<TraitDynamicConstraint> constraints = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = requiredCompound(tags, i, "dynamicConstraints");
            ListTag argumentTags = checkedList(tag, "arguments", "argumentCount", MAX_CONSTRAINTS, counter, Tag.TAG_STRING);
            List<String> arguments = new ArrayList<>(argumentTags.size());
            for (int argumentIndex = 0; argumentIndex < argumentTags.size(); argumentIndex++) {
                arguments.add(requiredString(argumentTags, argumentIndex, "arguments"));
            }
            constraints.add(new TraitDynamicConstraint(requiredString(tag, "type"), arguments));
        }
        return constraints;
    }

    private static ListTag checkedList(CompoundTag owner, String key, String countKey, int maximum,
                                       Counter counter, byte elementType) {
        Tag raw = owner.get(key);
        if (!(raw instanceof ListTag tags)) throw new IllegalArgumentException("Missing list: " + key);
        if (!tags.isEmpty() && tags.getElementType() != elementType) {
            throw new IllegalArgumentException("Invalid " + key + " element type");
        }
        int count = requiredNonnegative(owner, countKey);
        if (tags.size() != count) throw new IllegalArgumentException("Mismatched " + key + " count");
        if (count > maximum) throw new IllegalArgumentException("Too many " + key + ": " + count);
        counter.add(count);
        return tags;
    }

    private static CompoundTag requiredCompound(ListTag tags, int index, String name) {
        Tag tag = tags.get(index);
        if (!(tag instanceof CompoundTag compound)) throw new IllegalArgumentException("Invalid " + name + " entry");
        return compound;
    }

    private static String requiredString(ListTag tags, int index, String name) {
        Tag tag = tags.get(index);
        if (!(tag instanceof StringTag string)) throw new IllegalArgumentException("Invalid " + name + " entry");
        return checkedString(string.getAsString(), name);
    }

    private static String requiredString(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_STRING)) throw new IllegalArgumentException("Missing string: " + key);
        return checkedString(owner.getString(key), key);
    }

    private static String checkedString(String value, String name) {
        validateString(value, name);
        return value;
    }

    private static void validateString(String value, String name) {
        if (value == null || value.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException("Invalid " + name + " length");
        }
    }

    private static int requiredNonnegative(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_INT)) throw new IllegalArgumentException("Missing integer: " + key);
        int value = owner.getInt(key);
        validateNonnegative(value, key);
        return value;
    }

    private static long requiredLong(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_LONG)) throw new IllegalArgumentException("Missing long: " + key);
        return owner.getLong(key);
    }

    private static boolean requiredBoolean(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_BYTE)) throw new IllegalArgumentException("Missing boolean: " + key);
        return owner.getBoolean(key);
    }

    private static double requiredFinite(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_DOUBLE)) throw new IllegalArgumentException("Missing double: " + key);
        double value = owner.getDouble(key);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite " + key);
        return value;
    }

    private static double requiredProbability(CompoundTag owner, String key) {
        double value = requiredFinite(owner, key);
        validateProbability(value);
        return value;
    }

    private static ResourceLocation optionalId(CompoundTag owner, String key) {
        return owner.contains(key, Tag.TAG_STRING) ? parseId(requiredString(owner, key)) : null;
    }

    private static void putOptionalId(CompoundTag owner, String key, ResourceLocation id) {
        if (id != null) owner.putString(key, idString(id, key));
    }

    private static String idString(ResourceLocation id, String name) {
        if (id == null) throw new IllegalArgumentException("Missing resource id: " + name);
        String value = id.toString();
        parseId(value);
        return value;
    }

    private static void putNonnegative(CompoundTag owner, String key, int value) {
        validateNonnegative(value, key);
        owner.putInt(key, value);
    }

    private static void putMaxTraitCount(CompoundTag owner, int value) {
        if (value < -1) throw new IllegalArgumentException("maxTraitCount must be at least -1");
        owner.putInt("maxTraitCount", value);
    }

    private static int requiredMaxTraitCount(CompoundTag owner) {
        if (!owner.contains("maxTraitCount", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing integer: maxTraitCount");
        }
        int value = owner.getInt("maxTraitCount");
        if (value < -1) throw new IllegalArgumentException("maxTraitCount must be at least -1");
        return value;
    }

    private static void validateNonnegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
    }

    private static void putFinite(CompoundTag owner, String key, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite " + key);
        owner.putDouble(key, value);
    }

    private static void putProbability(CompoundTag owner, String key, double value) {
        validateProbability(value);
        owner.putDouble(key, value);
    }

    private static void checkSize(List<?> entries, int maximum, String name, Counter counter) {
        if (entries == null || entries.size() > maximum) {
            throw new IllegalArgumentException("Too many " + name);
        }
        counter.add(entries.size());
    }

    private static final class Counter {
        private int entries;

        private void add(int count) {
            if (count > MAX_TOTAL_ENTRIES - entries) {
                throw new IllegalArgumentException("Too many total entries");
            }
            entries += count;
        }
    }
}
