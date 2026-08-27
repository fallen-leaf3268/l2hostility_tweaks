package com.l2hostility_tweaks.client.tooltip;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HFEnchantments;
import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import dev.xkmc.l2hostility.init.data.LangData;
import dev.xkmc.l2hostility.init.registrate.LHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TooltipPipeline {

    private static final String REPRINT_DESC = "enchantment.l2hostility_tweaks.reprint_counter.desc";
    private static final String REPRINT_DESC_ANY = "enchantment.l2hostility_tweaks.reprint_counter.desc_any";
    private static final String REPRINT_DESC_ARMOR = "enchantment.l2hostility_tweaks.reprint_counter.desc_armor";
    private static final Set<String> REPRINT_DESCRIPTIONS =
            Set.of(REPRINT_DESC, REPRINT_DESC_ANY, REPRINT_DESC_ARMOR);
    private static final String ABYSS_DESC = "enchantment.l2hostility_tweaks.abyss_pocket.desc";
    private static final String GLUTTONY_DESC = "enchantment.l2hostility_tweaks.gluttony_pocket.desc";
    private static final String SPLIT_UPSTREAM_DESC = "enchantment.l2hostility.split_suppressor.desc";
    private static final String SPLIT_DESC =
            "enchantment.l2hostility_tweaks.split_suppressor.desc";
    private static final Set<String> SPLIT_DESCRIPTIONS =
            Set.of(SPLIT_UPSTREAM_DESC, SPLIT_DESC);
    private static final ResourceLocation SPLIT_SUPPRESSOR_ID =
            new ResourceLocation("l2hostility", "split_suppressor");
    private static final ResourceLocation ABRAHADABRA_ID =
            new ResourceLocation("l2hostility", "abrahadabra");
    private static final ResourceLocation DETECTOR_GLASSES_ID =
            new ResourceLocation("l2hostility", "detector_glasses");
    private static final ResourceLocation RESTORATION_POCKET_ID =
            new ResourceLocation("l2hostility", "pocket_of_restoration");
    private static final String ABRAHADABRA_TOOLTIP = "tooltip.l2hostility_tweaks.abrahadabra_minion";
    private static final String GLOW_ENABLED = "tooltip.l2hostility_tweaks.glow_enabled";
    private static final String GLOW_DISABLED = "tooltip.l2hostility_tweaks.glow_disabled";
    private static final String ESSENCE_TOOLTIP = "tooltip.l2hostility_tweaks.essence_use";
    private static final String SEALED_ITEM_TOOLTIP = "l2hostility.tooltip.sealed_item";
    private static final String RESTORATION_POCKET_DESCRIPTION =
            "l2hostility.item.equipment.pocket_of_restoration";

    private TooltipPipeline() {
    }

    public static void apply(ItemStack stack, @Nullable Player player, TooltipFlag flag,
                             List<Component> tooltip) {
        if (TooltipComponents.isVisible(stack, ItemStack.TooltipPart.ENCHANTMENTS)) {
            applyDescriptions(stack, EnchantmentHelper.getEnchantments(stack), tooltip);
        }
        if (TooltipComponents.isVisible(stack, ItemStack.TooltipPart.ADDITIONAL)) {
            applyItemDetails(stack, tooltip);
        }
    }

    static void applyDescriptions(ItemStack stack, Map<Enchantment, Integer> enchantments,
                                  List<Component> tooltip) {
        Enchantment reprint = L2HFEnchantments.REPRINT_COUNTER.get();
        int reprintLevel = enchantments.getOrDefault(reprint, 0);
        if (reprintLevel > 0) {
            applyDescription(tooltip, enchantments, reprint, REPRINT_DESCRIPTIONS,
                    reprintDescription(stack.getItem() instanceof ArmorItem,
                            stack.is(Items.ENCHANTED_BOOK), reprintLevel,
                            L2HConfig.getDisplayAntiReprintReduction()));
        }

        applyStaticDescription(tooltip, enchantments, L2HFEnchantments.ABYSS_POCKET.get(), ABYSS_DESC);
        applyStaticDescription(tooltip, enchantments, L2HFEnchantments.GLUTTONY_POCKET.get(), GLUTTONY_DESC);

        Enchantment splitSuppressor = ForgeRegistries.ENCHANTMENTS.getValue(SPLIT_SUPPRESSOR_ID);
        if (splitSuppressor != null) {
            applyDescription(tooltip, enchantments, splitSuppressor, SPLIT_DESCRIPTIONS,
                    splitSuppressorDescription());
        }
    }

    static Component splitSuppressorDescription() {
        return Component.translatable(SPLIT_DESC).withStyle(ChatFormatting.GRAY);
    }

    static Set<String> splitSuppressorDescriptionKeys() {
        return SPLIT_DESCRIPTIONS;
    }

    static void applyDescription(List<Component> tooltip, Map<Enchantment, Integer> enchantments,
                                 Enchantment enchantment, Collection<String> descriptionKeys,
                                 Component description) {
        if (!enchantments.containsKey(enchantment)) {
            return;
        }
        List<String> enchantmentNames = enchantments.keySet().stream()
                .map(Enchantment::getDescriptionId)
                .toList();
        TooltipComponents.upsert(tooltip, descriptionKeys, enchantment.getDescriptionId(),
                enchantmentNames, description);
    }

    static Component reprintDescription(boolean armor, boolean enchantedBook, int level,
                                        double reductionPerLevel) {
        if (!armor && !enchantedBook) {
            return Component.translatable(REPRINT_DESC_ANY).withStyle(ChatFormatting.GRAY);
        }
        double reduction = reductionPerLevel * level;
        if (enchantedBook) {
            reduction = Math.min(reduction, 0.8);
        }
        Component number = Component.literal(String.format(Locale.ROOT, "%.0f%%", reduction * 100))
                .withStyle(ChatFormatting.AQUA);
        String key = armor ? REPRINT_DESC_ARMOR : REPRINT_DESC;
        return Component.translatable(key, number).withStyle(ChatFormatting.GRAY);
    }

    static void applyStaticItemTooltip(List<Component> tooltip, Collection<String> keys,
                                       Component description) {
        TooltipComponents.upsertOrAppend(tooltip, keys, description);
    }

    private static void applyItemDetails(ItemStack stack, List<Component> tooltip) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ABRAHADABRA_ID.equals(itemId)) {
            applyStaticItemTooltip(tooltip, Set.of(ABRAHADABRA_TOOLTIP),
                    Component.translatable(ABRAHADABRA_TOOLTIP).withStyle(ChatFormatting.GOLD));
        }
        if (DETECTOR_GLASSES_ID.equals(itemId)) {
            boolean disabled = stack.hasTag() && stack.getTag().getBoolean("DetectorGlowDisabled");
            String key = disabled ? GLOW_DISABLED : GLOW_ENABLED;
            applyStaticItemTooltip(tooltip, Set.of(GLOW_ENABLED, GLOW_DISABLED),
                    Component.translatable(key)
                            .withStyle(disabled ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        if (stack.is(LHItems.HOSTILITY_ESSENCE.get())) {
            applyStaticItemTooltip(tooltip, Set.of(ESSENCE_TOOLTIP),
                    Component.translatable(ESSENCE_TOOLTIP, L2HConfig.getDisplayBottleOfCurseLevel())
                            .withStyle(ChatFormatting.GRAY));
        }
        if (RESTORATION_POCKET_ID.equals(itemId)) {
            addRestorationPocketContents(stack, tooltip);
        }
    }

    private static void addRestorationPocketContents(ItemStack stack, List<Component> tooltip) {
        int gluttonyLevel = EnchantmentHelper.getTagEnchantmentLevel(
                L2HFEnchantments.GLUTTONY_POCKET.get(), stack);
        CompoundTag tag = stack.getTag();
        if (gluttonyLevel <= 0 || tag == null) {
            return;
        }

        int sealedItemLine = findTranslationLine(tooltip, SEALED_ITEM_TOOLTIP);
        if (sealedItemLine >= 0) {
            int insertAt = Math.min(sealedItemLine + 2, tooltip.size());
            addExtraPocketSlots(tag, gluttonyLevel, tooltip, insertAt);
            return;
        }

        int descriptionLine = findTranslationLine(tooltip, RESTORATION_POCKET_DESCRIPTION);
        int insertAt = descriptionLine >= 0 ? descriptionLine + 1 : tooltip.size();
        tooltip.add(insertAt++, LangData.TOOLTIP_SEAL_DATA.get().withStyle(ChatFormatting.GRAY));
        insertAt = addStoredItem(tag, "UnsealRoot", tooltip, insertAt);
        addExtraPocketSlots(tag, gluttonyLevel, tooltip, insertAt);
    }

    private static void addExtraPocketSlots(CompoundTag tag, int gluttonyLevel,
                                            List<Component> tooltip, int insertAt) {
        for (int i = 1; i <= gluttonyLevel; i++) {
            insertAt = addStoredItem(tag, "UnsealRoot_" + i, tooltip, insertAt);
        }
    }

    private static int addStoredItem(CompoundTag tag, String key,
                                     List<Component> tooltip, int insertAt) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return insertAt;
        }
        ItemStack stored = ItemStack.of(tag.getCompound(key).getCompound(SealedItem.DATA));
        if (!stored.isEmpty()) {
            tooltip.add(insertAt++, stored.getHoverName());
        }
        return insertAt;
    }

    private static int findTranslationLine(List<Component> tooltip, String key) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (TooltipComponents.containsTranslation(tooltip.get(i), Set.of(key))) {
                return i;
            }
        }
        return -1;
    }

    private static void applyStaticDescription(List<Component> tooltip,
                                               Map<Enchantment, Integer> enchantments,
                                               Enchantment enchantment, String descriptionKey) {
        applyDescription(tooltip, enchantments, enchantment, Set.of(descriptionKey),
                Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
    }
}
