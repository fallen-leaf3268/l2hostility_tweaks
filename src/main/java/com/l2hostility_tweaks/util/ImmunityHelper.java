package com.l2hostility_tweaks.util;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ImmunityHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static TagKey<MobTrait> forceImmuneTraitTag;
    private static TagKey<MobTrait> gravityImmuneTraitTag;
    private static TagKey<MobTrait> playerSelfBlacklistTag;
    private static final ConcurrentHashMap<String, Boolean> traitTagCache = new ConcurrentHashMap<>();
    private static final int TRAIT_TAG_CACHE_MAX = 2000;

    private static WeakReference<LivingEntity> cachedEntityForceRef = new WeakReference<>(null);
    private static int cacheTickForce = -1;
    private static boolean cachedImmuneToForce;

    private static WeakReference<LivingEntity> cachedEntityGravityRef = new WeakReference<>(null);
    private static int cacheTickGravity = -1;
    private static boolean cachedImmuneToGravity;

    private static void discoverTraitRegistry() {
        if (forceImmuneTraitTag != null) return;
        var traitRegistry = TraitDisableHelper.getTraitRegistry();
        if (traitRegistry != null) {
            forceImmuneTraitTag = TagKey.create(traitRegistry.key(), new ResourceLocation("l2hostility_tweaks", "immune_to_force"));
            gravityImmuneTraitTag = TagKey.create(traitRegistry.key(), new ResourceLocation("l2hostility_tweaks", "immune_to_gravity"));
            playerSelfBlacklistTag = TagKey.create(traitRegistry.key(), new ResourceLocation("l2hostility_tweaks", "player_self_blacklist"));
        }
    }

    public static boolean hasCurioWithTag(LivingEntity entity, TagKey<Item> tag) {
        return hasItemWithTag(entity, tag);
    }

    private static boolean hasItemWithTag(LivingEntity entity, TagKey<Item> tag) {
        return CuriosApi.getCuriosInventory(entity).resolve().map(handler -> {
            for (var stacksHandler : handler.getCurios().values()) {
                var stacks = stacksHandler.getStacks();
                int slots = stacks.getSlots();
                for (int i = 0; i < slots; i++) {
                    var stack = stacks.getStackInSlot(i);
                    if (stack.is(tag)) return true;
                }
            }
            return false;
        }).orElse(false);
    }

    private static boolean hasTraitInTag(LivingEntity entity, TagKey<MobTrait> tag) {
        if (TraitDisableHelper.getTraitRegistry() == null || tag == null) return false;
        try {
            MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
            if (cap == null) return false;
            for (var entry : cap.traits.entrySet()) {
                if (entry.getValue() > 0) {
                    MobTrait trait = entry.getKey();
                    String traitId = trait.getID();
                    Boolean cached = traitTagCache.get(traitId + ":" + tag.location());
                    if (cached != null) {
                        if (cached) return true;
                        continue;
                    }
                    boolean isInTag = TraitDisableHelper.getTraitRegistry().getHolder(TraitDisableHelper.getTraitRegistry().getResourceKey(trait).orElse(null))
                            .map(h -> h.is(tag)).orElse(false);
                    if (traitTagCache.size() >= TRAIT_TAG_CACHE_MAX) {
                        traitTagCache.clear();
                    }
                    traitTagCache.put(traitId + ":" + tag.location(), isInTag);
                    if (isInTag) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isImmuneToForce(LivingEntity entity) {
        if (entity.tickCount == cacheTickForce && entity == cachedEntityForceRef.get()) {
            return cachedImmuneToForce;
        }
        boolean result = computeImmuneToForce(entity);
        cacheTickForce = entity.tickCount;
        cachedEntityForceRef = new WeakReference<>(entity);
        cachedImmuneToForce = result;
        return result;
    }

    public static boolean isImmuneToGravity(LivingEntity entity) {
        if (entity.tickCount == cacheTickGravity && entity == cachedEntityGravityRef.get()) {
            return cachedImmuneToGravity;
        }
        boolean result = computeImmuneToGravity(entity);
        cacheTickGravity = entity.tickCount;
        cachedEntityGravityRef = new WeakReference<>(entity);
        cachedImmuneToGravity = result;
        return result;
    }

    public static boolean isSelfBlacklisted(MobTrait trait) {
        TagKey<MobTrait> tag = resolveAfterDiscovery(
                ImmunityHelper::discoverTraitRegistry, () -> playerSelfBlacklistTag);
        if (TraitDisableHelper.getTraitRegistry() == null || tag == null) return false;
        return TraitDisableHelper.getTraitRegistry().getHolder(TraitDisableHelper.getTraitRegistry().getResourceKey(trait).orElse(null))
                .map(h -> h.is(tag)).orElse(false);
    }

    public static boolean isImmuneToTraitTick(LivingEntity entity, MobTrait trait) {
        TagKey<MobTrait> forceTag = resolveAfterDiscovery(
                ImmunityHelper::discoverTraitRegistry, () -> forceImmuneTraitTag);
        TagKey<MobTrait> gravityTag = resolveAfterDiscovery(
                ImmunityHelper::discoverTraitRegistry, () -> gravityImmuneTraitTag);
        if (isImmuneToForce(entity) && isTraitInTag(trait, forceTag)) return true;
        if (isImmuneToGravity(entity) && isTraitInTag(trait, gravityTag)) return true;
        return false;
    }

    private static boolean isTraitInTag(MobTrait trait, TagKey<MobTrait> tag) {
        if (TraitDisableHelper.getTraitRegistry() == null || tag == null) return false;
        return TraitDisableHelper.getTraitRegistry()
                .getHolder(TraitDisableHelper.getTraitRegistry().getResourceKey(trait).orElse(null))
                .map(h -> h.is(tag)).orElse(false);
    }

    private static boolean computeImmuneToForce(LivingEntity entity) {
        if (hasItemWithTag(entity, ItemTags.IMMUNE_TO_FORCE)) {
            LOGGER.debug("[ForceImmunity] Force immunity (Curios tag) for {}", entity.getName().getString());
            return true;
        }
        TagKey<MobTrait> tag = resolveAfterDiscovery(
                ImmunityHelper::discoverTraitRegistry, () -> forceImmuneTraitTag);
        if (hasTraitInTag(entity, tag)) {
            LOGGER.debug("[ForceImmunity] Force immunity (trait tag) for {}", entity.getName().getString());
            return true;
        }
        return false;
    }

    private static boolean computeImmuneToGravity(LivingEntity entity) {
        if (hasItemWithTag(entity, ItemTags.IMMUNE_TO_GRAVITY)) {
            LOGGER.debug("[GravityImmunity] Gravity immunity (Curios tag) for {}", entity.getName().getString());
            return true;
        }
        TagKey<MobTrait> tag = resolveAfterDiscovery(
                ImmunityHelper::discoverTraitRegistry, () -> gravityImmuneTraitTag);
        if (hasTraitInTag(entity, tag)) {
            LOGGER.debug("[GravityImmunity] Gravity immunity (trait tag) for {}", entity.getName().getString());
            return true;
        }
        return false;
    }

    static <T> T resolveAfterDiscovery(Runnable discovery, Supplier<T> refreshedValue) {
        discovery.run();
        return refreshedValue.get();
    }

    public static void invalidateTagCaches() {
        traitTagCache.clear();
        cachedEntityForceRef = new WeakReference<>(null);
        cacheTickForce = -1;
        cachedImmuneToForce = false;
        cachedEntityGravityRef = new WeakReference<>(null);
        cacheTickGravity = -1;
        cachedImmuneToGravity = false;
    }

    private static final class ItemTags {
        private static final TagKey<Item> IMMUNE_TO_FORCE = TagKey.create(
                Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "immune_to_force"));
        private static final TagKey<Item> IMMUNE_TO_GRAVITY = TagKey.create(
                Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "immune_to_gravity"));
    }
}
