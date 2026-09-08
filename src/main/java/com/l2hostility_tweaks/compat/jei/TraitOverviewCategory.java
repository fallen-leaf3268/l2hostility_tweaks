package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.client.TraitListTooltip;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TraitOverviewCategory implements IRecipeCategory<TraitSpawnIndexSnapshot.MobTraitOverview> {

    public static final RecipeType<TraitSpawnIndexSnapshot.MobTraitOverview> TYPE = JeiRuntimeBridge.PAGE_TYPE;
    public static final int WIDTH = 176;
    public static final int HEIGHT = 124;
    static final int MOB_CENTER_X = 32;
    static final int MOB_SLOT_X = 4;
    static final int MOB_SLOT_Y = 12;
    static final int MOB_RENDERER_WIDTH = 56;
    static final int MOB_RENDERER_HEIGHT = 72;
    static final int RIGHT_COLUMN_CENTER_X = 120;
    static final int POOL_SLOT_X = 112;
    static final int POOL_SLOT_Y = 12;
    static final int BLOCKED_SLOT_X = 112;
    static final int BLOCKED_SLOT_Y = 52;
    static final int PRESET_SLOT_X = 112;
    static final int PRESET_SLOT_Y = 92;
    static final int CONFIG_TEXT_X = 4;
    static final int CONFIG_TEXT_Y = 88;
    static final int DYNAMIC_TEXT_Y = 100;
    static final int DIFFICULTY_TEXT_Y = 112;
    static final int BLOCKED_HOVER_X = 112;
    static final int BLOCKED_HOVER_END_X = 128;
    static final int BLOCKED_HOVER_Y = 52;
    static final int BLOCKED_HOVER_END_Y = 68;

    private final IDrawable background;
    private final MobIngredientRenderer mobRenderer =
            new MobIngredientRenderer(MOB_RENDERER_WIDTH, MOB_RENDERER_HEIGHT);
    private final MobIngredientHelper mobHelper = new MobIngredientHelper();

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
        builder.addSlot(RecipeIngredientRole.INPUT, MOB_SLOT_X, MOB_SLOT_Y)
                .addIngredient(MobIngredient.TYPE, new MobIngredient(recipe.entityId()))
                .setCustomRenderer(MobIngredient.TYPE, mobRenderer)
                .setSlotName("mob")
                .addTooltipCallback((slot, tooltip) -> addGuaranteedPresetTooltip(recipe, tooltip));

        if (shouldRenderPresets(recipe)) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, PRESET_SLOT_X, PRESET_SLOT_Y)
                    .addItemStacks(resolveStacks(recipe.presets().stream()
                            .map(TraitSpawnIndexSnapshot.PresetTraitView::itemId).distinct().toList()))
                    .setSlotName("presets")
                    .addTooltipCallback((slot, tooltip) -> addPresetTooltip(recipe, slot, tooltip));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, POOL_SLOT_X, POOL_SLOT_Y)
                .addItemStacks(resolveStacks(recipe.pool().stream()
                        .map(TraitSpawnIndexSnapshot.PoolTraitView::itemId).distinct().toList()))
                .setSlotName("pool")
                .addTooltipCallback((slot, tooltip) -> {
                    tooltip.add(TraitListTooltip.marker(TraitOverviewPresentation.poolTraitIds(recipe)));
                    addPoolTooltip(recipe, slot, tooltip);
                });
    }

    @Override
    public void draw(TraitSpawnIndexSnapshot.MobTraitOverview recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        mobRenderer.setMousePosition(
                mouseX - (MOB_SLOT_X + MOB_RENDERER_WIDTH * 0.5D),
                mouseY - (MOB_SLOT_Y + MOB_RENDERER_HEIGHT * 0.5D));
        TraitOverviewPresentation.Counts counts = TraitOverviewPresentation.counts(recipe);
        String entityName = mobHelper.getDisplayName(new MobIngredient(recipe.entityId()));
        Component entityTitle = Component.literal(font.plainSubstrByWidth(entityName, MOB_RENDERER_WIDTH));
        drawCenteredNoShadow(guiGraphics, font, entityTitle, MOB_CENTER_X, 0, 0xFF555555);
        drawCenteredNoShadow(guiGraphics, font,
                Component.translatable("jei.l2hostility_tweaks.pool", counts.pool()),
                RIGHT_COLUMN_CENTER_X, 0, 0xFF555555);
        drawCenteredNoShadow(guiGraphics, font,
                Component.translatable("jei.l2hostility_tweaks.blocked", counts.blocked()),
                RIGHT_COLUMN_CENTER_X, 40, 0xFF555555);
        if (shouldRenderPresets(recipe)) {
            drawCenteredNoShadow(guiGraphics, font,
                    Component.translatable("jei.l2hostility_tweaks.preset", counts.presets()),
                    RIGHT_COLUMN_CENTER_X, 80, 0xFF555555);
        }
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.config_source", counts.configs()),
                CONFIG_TEXT_X, CONFIG_TEXT_Y, 0xFF555555, false);
        guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.dynamic", counts.dynamicConstraints()),
                CONFIG_TEXT_X, DYNAMIC_TEXT_Y, 0xFF777777, false);
        recipe.configs().stream().findFirst().ifPresent(config -> guiGraphics.drawString(font,
                Component.translatable("jei.l2hostility_tweaks.difficulty", config.minDifficulty(), config.baseDifficulty()),
                CONFIG_TEXT_X, DIFFICULTY_TEXT_Y, 0xFF777777, false));

        TraitSpawnIndexSnapshot.BlockedTraitView blocked = currentBlocked(recipe);
        if (blocked != null) {
            ItemStack blockedStack = stack(blocked.itemId()).orElse(ItemStack.EMPTY);
            if (!blockedStack.isEmpty()) guiGraphics.renderItem(blockedStack, BLOCKED_SLOT_X, BLOCKED_SLOT_Y);
        }
    }

    @Override
    public List<Component> getTooltipStrings(TraitSpawnIndexSnapshot.MobTraitOverview recipe,
                                             IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX < BLOCKED_HOVER_X || mouseX >= BLOCKED_HOVER_END_X
                || mouseY < BLOCKED_HOVER_Y || mouseY >= BLOCKED_HOVER_END_Y) return List.of();
        TraitSpawnIndexSnapshot.BlockedTraitView blocked = currentBlocked(recipe);
        if (blocked == null) return List.of();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(TraitListTooltip.marker(TraitOverviewPresentation.blockedTraitIds(recipe)));
        tooltip.add(traitDescription(blocked.traitId(), null));
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

    private static void addGuaranteedPresetTooltip(
            TraitSpawnIndexSnapshot.MobTraitOverview recipe, List<Component> tooltip) {
        List<TraitListTooltip.Entry> entries = TraitOverviewPresentation.guaranteedPresets(recipe).stream()
                .map(preset -> new TraitListTooltip.Entry(preset.traitId(), preset.freeRank()))
                .toList();
        if (!entries.isEmpty()) tooltip.add(TraitListTooltip.textMarker(entries));
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

    private static Component traitDescription(ResourceLocation traitId, Integer rank) {
        MobTrait trait = LHTraits.TRAITS.get().getValue(traitId);
        if (trait != null) return trait.getFullDesc(rank);
        return Component.literal(traitId.toString());
    }

    private static void drawCenteredNoShadow(GuiGraphics graphics, Font font, Component text,
                                             int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    static boolean shouldRenderPresets(TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        return !recipe.presets().isEmpty();
    }

    private static TraitSpawnIndexSnapshot.BlockedTraitView currentBlocked(
            TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        long tick = Optional.ofNullable(Minecraft.getInstance().level)
                .map(level -> level.getGameTime() / 40L).orElse(System.currentTimeMillis() / 2000L);
        return TraitOverviewPresentation.cycle(recipe.blocked(), tick);
    }
}
