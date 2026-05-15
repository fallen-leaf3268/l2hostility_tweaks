package com.l2hostilityfix.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;

public class ImmunityHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static TagKey<Item> getTag(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("l2hostilityfix", name));
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

    public static boolean isImmuneToForce(LivingEntity entity) {
        if (hasItemWithTag(entity, getTag("immune_to_force"))) {
            LOGGER.info("[ForceImmunity] Force immunity (tag) for {}", entity.getName().getString());
            return true;
        }
        return false;
    }

    public static boolean isImmuneToGravity(LivingEntity entity) {
        if (hasItemWithTag(entity, getTag("immune_to_gravity"))) {
            LOGGER.info("[ForceImmunity] Gravity immunity (tag) for {}", entity.getName().getString());
            return true;
        }
        return false;
    }
}
