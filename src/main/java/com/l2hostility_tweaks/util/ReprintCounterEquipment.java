package com.l2hostility_tweaks.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public final class ReprintCounterEquipment {

    private ReprintCounterEquipment() {
    }

    public static Summary scan(LivingEntity entity, Enchantment counter) {
        Accumulator accumulator = new Accumulator();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(counter, entity.getItemBySlot(slot));
            accumulator.accept(level, slot.getType() == EquipmentSlot.Type.ARMOR);
        }
        try {
            var inventory = CuriosApi.getCuriosInventory(entity).resolve();
            if (inventory.isPresent()) {
                for (var stacksHandler : inventory.get().getCurios().values()) {
                    var stacks = stacksHandler.getStacks();
                    for (int slot = 0; slot < stacks.getSlots(); slot++) {
                        int level = EnchantmentHelper.getItemEnchantmentLevel(
                                counter, stacks.getStackInSlot(slot));
                        accumulator.acceptCurio(level);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return accumulator.summary();
    }

    public static final class Accumulator {
        private int counterLevels;
        private int armorLevels;
        private final List<ReprintDamageCalculator.Point> curioPoints = new ArrayList<>();

        public void accept(int level, boolean armor) {
            if (level <= 0) return;
            counterLevels += level;
            if (armor) armorLevels += level;
        }

        public void acceptCurio(int level) {
            if (level <= 0) return;
            counterLevels += level;
            curioPoints.add(new ReprintDamageCalculator.Point(level, true));
        }

        public Summary summary() {
            return new Summary(counterLevels, armorLevels, List.copyOf(curioPoints));
        }
    }

    public record Summary(int counterLevels, int armorLevels,
                          List<ReprintDamageCalculator.Point> curioPoints) {
        public Summary(int counterLevels, int armorLevels) {
            this(counterLevels, armorLevels, List.of());
        }
    }
}
