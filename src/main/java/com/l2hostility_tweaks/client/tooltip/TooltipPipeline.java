package com.l2hostility_tweaks.client.tooltip;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.init.L2HFEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
    private static final String SPLIT_DESC = "enchantment.l2hostility.split_suppressor.desc";
    private static final ResourceLocation SPLIT_SUPPRESSOR_ID =
            new ResourceLocation("l2hostility", "split_suppressor");

    private TooltipPipeline() {
    }

    public static void apply(ItemStack stack, @Nullable Player player, TooltipFlag flag,
                             List<Component> tooltip) {
        if (TooltipComponents.isVisible(stack, ItemStack.TooltipPart.ENCHANTMENTS)) {
            applyDescriptions(stack, EnchantmentHelper.getEnchantments(stack), tooltip);
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
                            L2HConfig.getAntiReprintReduction()));
        }

        applyStaticDescription(tooltip, enchantments, L2HFEnchantments.ABYSS_POCKET.get(), ABYSS_DESC);
        applyStaticDescription(tooltip, enchantments, L2HFEnchantments.GLUTTONY_POCKET.get(), GLUTTONY_DESC);

        Enchantment splitSuppressor = ForgeRegistries.ENCHANTMENTS.getValue(SPLIT_SUPPRESSOR_ID);
        if (splitSuppressor != null) {
            applyStaticDescription(tooltip, enchantments, splitSuppressor, SPLIT_DESC);
        }
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

    private static void applyStaticDescription(List<Component> tooltip,
                                               Map<Enchantment, Integer> enchantments,
                                               Enchantment enchantment, String descriptionKey) {
        applyDescription(tooltip, enchantments, enchantment, Set.of(descriptionKey),
                Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
    }
}
