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

import java.util.concurrent.ConcurrentHashMap;

public class ImmunityHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static TagKey<MobTrait> forceImmuneTraitTag;
    private static TagKey<MobTrait> gravityImmuneTraitTag;
    private static TagKey<MobTrait> playerSelfBlacklistTag;
    private static final ConcurrentHashMap<String, Boolean> traitTagCache = new ConcurrentHashMap<>();
    private static final int TRAIT_TAG_CACHE_MAX = 2000;

    private static final TagKey<Item> IMMUNE_TO_FORCE_ITEM = TagKey.create(Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "immune_to_force"));
    private static final TagKey<Item> IMMUNE_TO_GRAVITY_ITEM = TagKey.create(Registries.ITEM, new ResourceLocation("l2hostility_tweaks", "immune_to_gravity"));

    private static LivingEntity cachedEntityForce;
    private static int cacheTickForce = -1;
    private static boolean cachedImmuneToForce;

    private static LivingEntity cachedEntityGravity;
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

    private static boolean hasItemWithTag(LivingEntity entity, TagKey<Item> tag) {
        return CuriosApi.getCuriosInventory(entity).resolve().map(handler -> {
            for (var stacksHandler : handler.getCurios().values()) {
                var stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).is(tag)) return true;
                }
            }
            return false;
        }).orElse(false);
    }

    private static boolean hasTraitInTag(LivingEntity entity, TagKey<MobTrait> tag) {
        discoverTraitRegistry();
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
        if (entity.tickCount == cacheTickForce && entity == cachedEntityForce) {
            return cachedImmuneToForce;
        }
        boolean result = computeImmuneToForce(entity);
        cacheTickForce = entity.tickCount;
        cachedEntityForce = entity;
        cachedImmuneToForce = result;
        return result;
    }

    public static boolean isImmuneToGravity(LivingEntity entity) {
        if (entity.tickCount == cacheTickGravity && entity == cachedEntityGravity) {
            return cachedImmuneToGravity;
        }
        boolean result = computeImmuneToGravity(entity);
        cacheTickGravity = entity.tickCount;
        cachedEntityGravity = entity;
        cachedImmuneToGravity = result;
        return result;
    }

    public static boolean isSelfBlacklisted(MobTrait trait) {
        discoverTraitRegistry();
        if (TraitDisableHelper.getTraitRegistry() == null || playerSelfBlacklistTag == null) return false;
        return TraitDisableHelper.getTraitRegistry().getHolder(TraitDisableHelper.getTraitRegistry().getResourceKey(trait).orElse(null))
                .map(h -> h.is(playerSelfBlacklistTag)).orElse(false);
    }

    public static boolean isImmuneToTraitTick(LivingEntity entity, MobTrait trait) {
        if (isImmuneToForce(entity) && isTraitInTag(trait, forceImmuneTraitTag)) return true;
        if (isImmuneToGravity(entity) && isTraitInTag(trait, gravityImmuneTraitTag)) return true;
        return false;
    }

    private static boolean isTraitInTag(MobTrait trait, TagKey<MobTrait> tag) {
        discoverTraitRegistry();
        if (TraitDisableHelper.getTraitRegistry() == null || tag == null) return false;
        return TraitDisableHelper.getTraitRegistry()
                .getHolder(TraitDisableHelper.getTraitRegistry().getResourceKey(trait).orElse(null))
                .map(h -> h.is(tag)).orElse(false);
    }

    private static boolean computeImmuneToForce(LivingEntity entity) {
        if (hasItemWithTag(entity, IMMUNE_TO_FORCE_ITEM)) {
            LOGGER.debug("[ForceImmunity] Force immunity (Curios tag) for {}", entity.getName().getString());
            return true;
        }
        if (hasTraitInTag(entity, forceImmuneTraitTag)) {
            LOGGER.debug("[ForceImmunity] Force immunity (trait tag) for {}", entity.getName().getString());
            return true;
        }
        return false;
    }

    private static boolean computeImmuneToGravity(LivingEntity entity) {
        if (hasItemWithTag(entity, IMMUNE_TO_GRAVITY_ITEM)) {
            LOGGER.debug("[GravityImmunity] Gravity immunity (Curios tag) for {}", entity.getName().getString());
            return true;
        }
        if (hasTraitInTag(entity, gravityImmuneTraitTag)) {
            LOGGER.debug("[GravityImmunity] Gravity immunity (trait tag) for {}", entity.getName().getString());
            return true;
        }
        return false;
    }
}
