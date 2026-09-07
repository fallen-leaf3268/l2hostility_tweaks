package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitDynamicConstraint;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TraitOverviewCategory implements IRecipeCategory<TraitSpawnIndexSnapshot.MobTraitOverview> {

    public static final RecipeType<TraitSpawnIndexSnapshot.MobTraitOverview> TYPE = JeiRuntimeBridge.PAGE_TYPE;
    public static final int WIDTH = 176;
    public static final int HEIGHT = 124;

    private final IDrawable background;
    private final MobIngredientRenderer mobRenderer = new MobIngredientRenderer(48);

    public TraitOverviewCategory(IJeiHelpers helpers) {
        background = helpers.getGuiHelper().createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<TraitSpawnIndexSnapshot.MobTraitOverview> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.l2hostility_tweaks.mob_traits.title");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TraitSpawnIndexSnapshot.MobTraitOverview recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 8)
                .addIngredient(MobIngredient.TYPE, new MobIngredient(recipe.entityId()))
                .setCustomRenderer(MobIngredient.TYPE, mobRenderer)
                .setSlotName("mob")
                .addTooltipCallback((slot, tooltip) -> addConfigTooltip(recipe, tooltip));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 72, 62)
                .addItemStacks(resolveStacks(recipe.presets().stream()
                        .map(TraitSpawnIndexSnapshot.PresetTraitView::itemId).distinct().toList()))
                .setSlotName("presets")
                .addTooltipCallback((slot, tooltip) -> addPresetTooltip(recipe, slot, tooltip));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 62)
                .addItemStacks(resolveStacks(recipe.pool().stream()
                        .map(TraitSpawnIndexSnapshot.PoolTraitView::itemId).distinct().toList()))
                .setSlotName("pool")
                .addTooltipCallback((slot, tooltip) -> addPoolTooltip(recipe, slot, tooltip));
    }

    @Override
    public void draw(TraitSpawnIndexSnapshot.MobTraitOverview recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        TraitOverviewPresentation.Counts counts = TraitOverviewPresentation.counts(recipe);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.mob"), 8, 0, 0xFF555555, false);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.preset", counts.presets()),
                62, 49, 0xFF555555, false);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.pool", counts.pool()),
                104, 49, 0xFF555555, false);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.blocked", counts.blocked()),
                140, 49, 0xFF555555, false);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.config_source", counts.configs()),
                8, 88, 0xFF555555, false);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.dynamic", counts.dynamicConstraints()),
                8, 100, 0xFF777777, false);
        recipe.configs().stream().findFirst().ifPresent(config -> guiGraphics.drawString(font,
                Component.translatable("jei.l2hostility_tweaks.difficulty", config.minDifficulty(), config.baseDifficulty()),
                8, 112, 0xFF777777, false));

        TraitSpawnIndexSnapshot.BlockedTraitView blocked = currentBlocked(recipe);
        if (blocked != null) {
            ItemStack blockedStack = stack(blocked.itemId()).orElse(ItemStack.EMPTY);
            if (!blockedStack.isEmpty()) guiGraphics.renderItem(blockedStack, 148, 62);
        }
    }

    @Override
    public List<Component> getTooltipStrings(TraitSpawnIndexSnapshot.MobTraitOverview recipe,
                                             IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX < 148 || mouseX >= 164 || mouseY < 62 || mouseY >= 78) return List.of();
        TraitSpawnIndexSnapshot.BlockedTraitView blocked = currentBlocked(recipe);
        if (blocked == null) return List.of();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("jei.l2hostility_tweaks.blocked_trait", blocked.traitId().toString())
                .withStyle(ChatFormatting.RED));
        blocked.contexts().forEach(context -> {
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.source",
                            TraitOverviewPresentation.formatNullableId(context.sourceId()))
                    .withStyle(ChatFormatting.GRAY));
            context.reasons().forEach(reason -> tooltip.add(Component.translatable(
                    TraitOverviewPresentation.blockReasonKey(reason)).withStyle(ChatFormatting.DARK_RED)));
        });
        return tooltip;
    }

    @Override
    public ResourceLocation getRegistryName(TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        return new ResourceLocation("l2hostility_tweaks", "mob_traits/" + recipe.entityId().getNamespace()
                + "/" + recipe.entityId().getPath());
    }

    private static void addConfigTooltip(TraitSpawnIndexSnapshot.MobTraitOverview recipe, List<Component> tooltip) {
        tooltip.add(Component.literal(recipe.entityId().toString()).withStyle(ChatFormatting.DARK_GRAY));
        for (TraitSpawnIndexSnapshot.EntityConfigView config : recipe.configs()) {
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.source",
                            TraitOverviewPresentation.formatNullableId(config.sourceId()))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.difficulty",
                    config.minDifficulty(), config.baseDifficulty()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.variation", config.variation()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.scale", config.scale()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.apply_chance",
                    TraitOverviewPresentation.formatPercent(config.applyChance())));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.trait_chance",
                    TraitOverviewPresentation.formatPercent(config.traitChance())));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.suppression", config.suppression()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.min_spawn_level", config.minSpawnLevel()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.max_level", config.maxLevel()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.max_trait_count", config.maxTraitCount()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.preset_only", config.presetTraitsOnly()));
            String condition = TraitOverviewPresentation.compactConditionJson(config.conditionJson());
            if (!condition.isEmpty()) tooltip.add(Component.translatable("jei.l2hostility_tweaks.condition", condition)
                    .withStyle(ChatFormatting.GRAY));
        }
        for (TraitDynamicConstraint constraint : recipe.dynamicConstraints()) {
            tooltip.add(Component.translatable(TraitOverviewPresentation.dynamicConstraintKey(constraint),
                    String.join(", ", constraint.arguments())).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void addPresetTooltip(TraitSpawnIndexSnapshot.MobTraitOverview recipe, IRecipeSlotView slot,
                                         List<Component> tooltip) {
        displayedItemId(slot).ifPresent(itemId -> recipe.presets().stream()
                .filter(preset -> preset.itemId().equals(itemId))
                .forEach(preset -> {
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.preset_chance",
                            TraitOverviewPresentation.formatPercent(preset.chance())).withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.free_rank", preset.freeRank()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.min_rank", preset.minRank()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.cap", preset.cap()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.condition_level", preset.conditionLevel()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.advancement",
                            TraitOverviewPresentation.formatNullableId(preset.advancementId())));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.source",
                                    TraitOverviewPresentation.formatNullableId(preset.sourceId()))
                            .withStyle(ChatFormatting.GRAY));
                    String condition = TraitOverviewPresentation.compactConditionJson(preset.conditionJson());
                    if (!condition.isEmpty()) tooltip.add(Component.translatable("jei.l2hostility_tweaks.condition", condition));
                    preset.dynamicConstraints().forEach(constraint -> tooltip.add(Component.translatable(
                            TraitOverviewPresentation.dynamicConstraintKey(constraint),
                            String.join(", ", constraint.arguments())).withStyle(ChatFormatting.YELLOW)));
                }));
    }

    private static void addPoolTooltip(TraitSpawnIndexSnapshot.MobTraitOverview recipe, IRecipeSlotView slot,
                                       List<Component> tooltip) {
        displayedItemId(slot).ifPresent(itemId -> recipe.pool().stream()
                .filter(pool -> pool.itemId().equals(itemId))
                .forEach(pool -> {
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.pool_weight", pool.weight())
                            .withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.min_level", pool.minLevel()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.cost", pool.cost()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.max_rank", pool.maxRank()));
                    tooltip.add(Component.translatable("jei.l2hostility_tweaks.no_fixed_pool_chance")
                            .withStyle(ChatFormatting.GRAY));
                }));
    }

    private static List<ItemStack> resolveStacks(List<ResourceLocation> itemIds) {
        List<ItemStack> stacks = new ArrayList<>();
        itemIds.forEach(itemId -> stack(itemId).ifPresent(stacks::add));
        return stacks;
    }

    private static Optional<ItemStack> stack(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId).filter(item -> item != Items.AIR)
                .map(Item::getDefaultInstance).filter(value -> !value.isEmpty());
    }

    private static Optional<ResourceLocation> displayedItemId(IRecipeSlotView slot) {
        return slot.getDisplayedItemStack().map(ItemStack::getItem).map(BuiltInRegistries.ITEM::getKey);
    }

    private static TraitSpawnIndexSnapshot.BlockedTraitView currentBlocked(
            TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        long tick = Optional.ofNullable(Minecraft.getInstance().level)
                .map(level -> level.getGameTime() / 40L).orElse(System.currentTimeMillis() / 2000L);
        return TraitOverviewPresentation.cycle(recipe.blocked(), tick);
    }
}
