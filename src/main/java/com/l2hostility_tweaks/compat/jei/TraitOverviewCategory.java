package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobEquipmentCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TraitOverviewCategory implements IRecipeCategory<TraitSpawnIndexSnapshot.MobTraitOverview> {

    public static final RecipeType<TraitSpawnIndexSnapshot.MobTraitOverview> TYPE = JeiRuntimeBridge.PAGE_TYPE;
    static final ResourceLocation ICON_ITEM_ID = new ResourceLocation("l2hostility", "teleport");
    public static final int WIDTH = 176;
    public static final int HEIGHT = 124;
    static final int MOB_CENTER_X = 32;
    static final int MOB_SLOT_X = 4;
    static final int MOB_SLOT_Y = 12;
    static final int MOB_RENDERER_WIDTH = 56;
    static final int MOB_RENDERER_HEIGHT = 72;
    static final int EQUIPMENT_SLOT_X = 77;
    static final int ARMOR_SLOT_Y = 12;
    static final int HANDS_SLOT_Y = 42;
    static final int CURIOS_SLOT_Y = 72;
    static final int RIGHT_COLUMN_CENTER_X = 120;
    static final int POOL_SLOT_X = 112;
    static final int POOL_SLOT_Y = 12;
    static final int BLOCKED_SLOT_X = 112;
    static final int BLOCKED_SLOT_Y = 52;
    static final int PRESET_SLOT_X = 112;
    static final int PRESET_SLOT_Y = 92;
    static final int CONFIG_TEXT_X = 4;
    static final int DIFFICULTY_TEXT_Y = 112;
    private final IDrawable background;
    private final IDrawable icon;
    private final MobIngredientRenderer mobRenderer =
            new MobIngredientRenderer(MOB_RENDERER_WIDTH, MOB_RENDERER_HEIGHT);
    private final MobIngredientHelper mobHelper = new MobIngredientHelper();

    public TraitOverviewCategory(IJeiHelpers helpers) {
        background = helpers.getGuiHelper().createBlankDrawable(WIDTH, HEIGHT);
        Item iconItem = BuiltInRegistries.ITEM.get(ICON_ITEM_ID);
        icon = helpers.getGuiHelper().createDrawableItemStack(new ItemStack(iconItem));
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
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TraitSpawnIndexSnapshot.MobTraitOverview recipe,
                          IFocusGroup focuses) {
        List<ResourceLocation> presetItemIds = recipe.presets().stream()
                .map(TraitSpawnIndexSnapshot.PresetTraitView::itemId).distinct().toList();
        List<ResourceLocation> poolItemIds = recipe.pool().stream()
                .map(TraitSpawnIndexSnapshot.PoolTraitView::itemId).distinct().toList();
        List<ResourceLocation> blockedItemIds = recipe.blocked().stream()
                .map(TraitSpawnIndexSnapshot.BlockedTraitView::itemId).distinct().toList();

        builder.addSlot(RecipeIngredientRole.INPUT, MOB_SLOT_X, MOB_SLOT_Y)
                .addIngredient(MobIngredient.TYPE, new MobIngredient(recipe.entityId()))
                .setCustomRenderer(MobIngredient.TYPE, mobRenderer)
                .setSlotName("mob")
                .addTooltipCallback((slot, tooltip) -> {
                    tooltip.clear();
                    tooltip.addAll(mobTooltip(recipe));
                });

        addEquipmentSlot(builder, recipe, MobEquipmentCategory.ARMOR,
                ARMOR_SLOT_Y, "equipment_armor");
        addEquipmentSlot(builder, recipe, MobEquipmentCategory.HANDS,
                HANDS_SLOT_Y, "equipment_hands");
        addEquipmentSlot(builder, recipe, MobEquipmentCategory.CURIOS,
                CURIOS_SLOT_Y, "equipment_curios");

        if (shouldRenderPresets(recipe)) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, PRESET_SLOT_X, PRESET_SLOT_Y)
                    .addItemStacks(resolveStacks(presetItemIds))
                    .setSlotName("presets")
                    .addTooltipCallback((slot, tooltip) -> TraitIngredientTooltipRegistry.activate(recipe,
                            TraitIngredientTooltipContext.Section.PRESET));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, POOL_SLOT_X, POOL_SLOT_Y)
                .addItemStacks(resolveStacks(poolItemIds))
                .setSlotName("pool")
                .addTooltipCallback((slot, tooltip) -> TraitIngredientTooltipRegistry.activate(recipe,
                        TraitIngredientTooltipContext.Section.POOL));

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, BLOCKED_SLOT_X, BLOCKED_SLOT_Y)
                .addItemStacks(resolveStacks(blockedItemIds))
                .setSlotName("blocked")
                .addTooltipCallback((slot, tooltip) -> TraitIngredientTooltipRegistry.activate(recipe,
                        TraitIngredientTooltipContext.Section.BLOCKED));

    }

    @Override
    public void draw(TraitSpawnIndexSnapshot.MobTraitOverview recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        mobRenderer.setMousePosition(
                mouseX - (MOB_SLOT_X + MOB_RENDERER_WIDTH * 0.5D),
                mouseY - (MOB_SLOT_Y + MOB_RENDERER_HEIGHT * 0.5D));
        TraitOverviewPresentation.Counts counts = TraitOverviewPresentation.counts(recipe);
        String entityName = TraitOverviewPresentation.mobDisplayName(recipe,
                mobHelper.getDisplayName(new MobIngredient(recipe.entityId())));
        Component entityTitle = compactMobTitle(font, entityName, recipe);
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
        recipe.configs().stream().findFirst().ifPresent(config -> {
            TraitOverviewPresentation.DifficultyRange range = TraitOverviewPresentation.difficultyRange(config);
            guiGraphics.drawString(font, Component.translatable("jei.l2hostility_tweaks.difficulty_range",
                            range.minimum(), range.maximum(), range.base()),
                    CONFIG_TEXT_X, DIFFICULTY_TEXT_Y, 0xFF777777, false);
        });

    }

    @Override
    public List<Component> getTooltipStrings(TraitSpawnIndexSnapshot.MobTraitOverview recipe,
                                             IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (isInsideMobPreview(mouseX, mouseY)) return mobTooltip(recipe);
        return List.of();
    }

    @Override
    public ResourceLocation getRegistryName(TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        ResourceLocation pageId = recipe.pageId();
        return new ResourceLocation("l2hostility_tweaks", "mob_traits/" + pageId.getNamespace()
                + "/" + pageId.getPath());
    }

    private List<Component> mobTooltip(TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(mobTitle(TraitOverviewPresentation.mobDisplayName(recipe,
                mobHelper.getDisplayName(new MobIngredient(recipe.entityId()))), recipe));
        addConfigTooltip(recipe, tooltip);
        return tooltip;
    }

    private static Component compactMobTitle(Font font, String entityName,
                                             TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        String key = TraitOverviewPresentation.mobVariantTitleKey(recipe);
        if (key.isEmpty()) {
            return Component.literal(font.plainSubstrByWidth(entityName, MOB_RENDERER_WIDTH));
        }
        String suffix = Component.translatable(key, "").getString();
        int nameWidth = Math.max(0, MOB_RENDERER_WIDTH - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(entityName, nameWidth) + suffix);
    }

    private static Component mobTitle(String entityName,
                                      TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        String key = TraitOverviewPresentation.mobVariantTitleKey(recipe);
        return key.isEmpty() ? Component.literal(entityName) : Component.translatable(key, entityName);
    }

    private static void addConfigTooltip(TraitSpawnIndexSnapshot.MobTraitOverview recipe, List<Component> tooltip) {
        for (TraitSpawnIndexSnapshot.EntityConfigView config : recipe.configs()) {
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.source",
                            TraitOverviewPresentation.formatEntityConfigPath(config.sourceId()))
                    .withStyle(ChatFormatting.GOLD));
            TraitOverviewPresentation.DifficultyRange range = TraitOverviewPresentation.difficultyRange(config);
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.difficulty_range",
                    range.minimum(), range.maximum(), range.base()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.variation", config.variation()));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.scale", config.scale()));
            if (TraitOverviewPresentation.usesGlobalMaxTraitCount(config)) {
                tooltip.add(Component.translatable("jei.l2hostility_tweaks.max_trait_count_global"));
            } else {
                tooltip.add(Component.translatable("jei.l2hostility_tweaks.max_trait_count", config.maxTraitCount()));
            }
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.apply_chance",
                    TraitOverviewPresentation.formatPercent(config.applyChance())));
            tooltip.add(Component.translatable("jei.l2hostility_tweaks.preset_only", config.presetTraitsOnly()));
        }
    }

    private static List<ItemStack> resolveStacks(List<ResourceLocation> itemIds) {
        List<ItemStack> stacks = new ArrayList<>();
        itemIds.forEach(itemId -> stack(itemId).ifPresent(stacks::add));
        return stacks;
    }

    private static void addEquipmentSlot(IRecipeLayoutBuilder builder,
                                         TraitSpawnIndexSnapshot.MobTraitOverview recipe,
                                         MobEquipmentCategory category,
                                         int y, String name) {
        List<ItemStack> stacks = recipe.equipment().stream()
                .filter(entry -> entry.category() == category)
                .map(entry -> entry.stack())
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (stacks.isEmpty()) return;
        builder.addSlot(RecipeIngredientRole.OUTPUT, EQUIPMENT_SLOT_X, y)
                .addItemStacks(stacks)
                .setSlotName(name)
                .addTooltipCallback((slot, tooltip) ->
                        TraitIngredientTooltipRegistry.activateEquipment(recipe, category));
    }

    private static Optional<ItemStack> stack(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId).filter(item -> item != Items.AIR)
                .map(Item::getDefaultInstance).filter(value -> !value.isEmpty());
    }

    private static void drawCenteredNoShadow(GuiGraphics graphics, Font font, Component text,
                                             int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    static boolean shouldRenderPresets(TraitSpawnIndexSnapshot.MobTraitOverview recipe) {
        return !recipe.presets().isEmpty();
    }

    static boolean isInsideMobPreview(double mouseX, double mouseY) {
        return mouseX >= MOB_SLOT_X && mouseX < MOB_SLOT_X + MOB_RENDERER_WIDTH
                && mouseY >= MOB_SLOT_Y && mouseY < MOB_SLOT_Y + MOB_RENDERER_HEIGHT;
    }

}
