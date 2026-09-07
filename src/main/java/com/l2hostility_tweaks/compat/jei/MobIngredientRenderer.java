package com.l2hostility_tweaks.compat.jei;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MobIngredientRenderer implements IIngredientRenderer<MobIngredient> {

    private final int size;
    private final MobIngredientHelper helper = new MobIngredientHelper();
    private final Map<ResourceLocation, LivingEntity> entities = new HashMap<>();
    private final Set<ResourceLocation> failures = new HashSet<>();
    private Level cachedLevel;

    public MobIngredientRenderer(int size) {
        if (size < 1) throw new IllegalArgumentException("size must be positive");
        this.size = size;
    }

    @Override
    public void render(GuiGraphics guiGraphics, MobIngredient ingredient) {
        ResourceLocation entityId = ingredient.entityId();
        if (failures.contains(entityId)) {
            renderFallback(guiGraphics, ingredient);
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            renderFallback(guiGraphics, ingredient);
            return;
        }
        try {
            LivingEntity entity = getOrCreate(entityId, level);
            if (entity == null) {
                failures.add(entityId);
                renderFallback(guiGraphics, ingredient);
                return;
            }
            float extent = Math.max(entity.getBbWidth(), entity.getBbHeight());
            int scale = Math.max(1, Math.round((size - 2) / Math.max(0.25F, extent)));
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                    guiGraphics, size / 2, size - 1, scale, 0.0F, 0.0F, entity);
        } catch (RuntimeException | LinkageError exception) {
            failures.add(entityId);
            entities.remove(entityId);
            renderFallback(guiGraphics, ingredient);
        }
    }

    @Override
    public List<Component> getTooltip(MobIngredient ingredient, TooltipFlag tooltipFlag) {
        String displayName = helper.getDisplayName(ingredient);
        Component id = Component.literal(ingredient.entityId().toString()).withStyle(ChatFormatting.DARK_GRAY);
        if (displayName.equals(ingredient.entityId().toString())) return List.of(id);
        return List.of(Component.literal(displayName), id);
    }

    @Override
    public int getWidth() {
        return size;
    }

    @Override
    public int getHeight() {
        return size;
    }

    private LivingEntity getOrCreate(ResourceLocation entityId, Level level) {
        if (cachedLevel != level) {
            entities.clear();
            cachedLevel = level;
        }
        LivingEntity cached = entities.get(entityId);
        if (cached != null) return cached;
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                .map(type -> type.create(level))
                .orElse(null);
        if (!(entity instanceof LivingEntity livingEntity)) return null;
        entities.put(entityId, livingEntity);
        return livingEntity;
    }

    private void renderFallback(GuiGraphics guiGraphics, MobIngredient ingredient) {
        guiGraphics.renderItem(new ItemStack(Items.BARRIER), Math.max(0, (size - 16) / 2), 0);
        if (size > 16) {
            String id = ingredient.entityId().toString();
            guiGraphics.drawString(Minecraft.getInstance().font, id, 1, size - 9, 0xFFFFFFFF, true);
        }
    }
}
