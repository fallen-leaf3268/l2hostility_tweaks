package com.l2hostility_tweaks.content;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.MovementSpeedCapCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

public class WalkingBootsItem extends Item implements ICurioItem {

    private static final UUID MOVEMENT_SPEED_CAP_ID =
            UUID.fromString("5e096b4f-0c23-47f0-bc93-cb91ce06b379");
    private static final UUID SPRINTING_SPEED_BOOST_ID =
            UUID.fromString("662a6b8d-da3e-4c1c-8813-96ea6097278d");

    public WalkingBootsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity().level().isClientSide) return;
        if (!(slotContext.entity() instanceof Player player)) return;
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        double externalValue = calculateExternalValue(attribute);
        OptionalDouble amount = MovementSpeedCapCalculator.calculateModifierAmount(
                externalValue, L2HConfig.getWalkingBootsMovementSpeedCap());
        AttributeModifier existing = attribute.getModifier(MOVEMENT_SPEED_CAP_ID);
        if (amount.isEmpty()) {
            if (existing != null) attribute.removeModifier(MOVEMENT_SPEED_CAP_ID);
            return;
        }
        if (existing != null && existing.getAmount() == amount.getAsDouble()) return;
        if (existing != null) attribute.removeModifier(MOVEMENT_SPEED_CAP_ID);
        attribute.addTransientModifier(new AttributeModifier(
                MOVEMENT_SPEED_CAP_ID, "walking_boots_speed_cap", amount.getAsDouble(),
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        AttributeInstance attribute = slotContext.entity().getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) attribute.removeModifier(MOVEMENT_SPEED_CAP_ID);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String cap = MovementSpeedCapCalculator.formatCap(
                L2HConfig.getWalkingBootsMovementSpeedCap());
        tooltip.add(Component.translatable("tooltip.l2hostility_tweaks.walking_boots", cap)
                .withStyle(ChatFormatting.GOLD));
    }

    private static double calculateExternalValue(AttributeInstance attribute) {
        return MovementSpeedCapCalculator.calculateExternalValue(
                attribute.getBaseValue(),
                modifierAmounts(attribute, AttributeModifier.Operation.ADDITION),
                modifierAmounts(attribute, AttributeModifier.Operation.MULTIPLY_BASE),
                modifierAmounts(attribute, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static List<Double> modifierAmounts(AttributeInstance attribute,
                                                AttributeModifier.Operation operation) {
        return attribute.getModifiers(operation).stream()
                .filter(WalkingBootsItem::isExternalModifier)
                .map(AttributeModifier::getAmount)
                .toList();
    }

    private static boolean isExternalModifier(AttributeModifier modifier) {
        return !modifier.getId().equals(MOVEMENT_SPEED_CAP_ID)
                && !modifier.getId().equals(SPRINTING_SPEED_BOOST_ID);
    }
}
