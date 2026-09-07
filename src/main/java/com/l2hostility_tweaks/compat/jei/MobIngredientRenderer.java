package com.l2hostility_tweaks.compat.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class MobIngredientRenderer implements IIngredientRenderer<MobIngredient>, AutoCloseable {

    private static final Field RENDER_SHADOW = ObfuscationReflectionHelper.findField(
            EntityRenderDispatcher.class, "f_114368_");
    private final int size;
    private final MobIngredientHelper helper = new MobIngredientHelper();
    private final EntityCache<Level, LivingEntity> entities = new EntityCache<>(Entity::discard);

    public MobIngredientRenderer(int size) {
        if (size < 1) throw new IllegalArgumentException("size must be positive");
        this.size = size;
    }

    @Override
    public void render(GuiGraphics guiGraphics, MobIngredient ingredient) {
        ResourceLocation entityId = ingredient.entityId();
        Level level = Minecraft.getInstance().level;
        entities.updateLevel(level);
        if (level == null || entities.hasFailed(entityId)) {
            renderFallback(guiGraphics, ingredient);
            return;
        }
        try {
            LivingEntity entity = getOrCreate(entityId, level);
            if (entity == null) {
                entities.fail(entityId);
                renderFallback(guiGraphics, ingredient);
                return;
            }
            PreviewLayout layout = PreviewLayout.calculate(size, entity.getBbWidth(), entity.getBbHeight(),
                    Util.getMillis());
            ScissorBounds scissor = ScissorBounds.calculate(guiGraphics.pose().last().pose(), size);
            ScopedState.use(
                    () -> guiGraphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom()),
                    guiGraphics::disableScissor,
                    () -> renderEntity(guiGraphics, layout, entity));
        } catch (RuntimeException | LinkageError exception) {
            entities.fail(entityId);
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

    public void clear() {
        entities.clear();
    }

    @Override
    public void close() {
        clear();
    }

    private LivingEntity getOrCreate(ResourceLocation entityId, Level level) {
        LivingEntity cached = entities.get(entityId);
        if (cached != null) return cached;
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                .map(type -> type.create(level))
                .orElse(null);
        if (!(entity instanceof LivingEntity livingEntity)) {
            if (entity != null) entity.discard();
            return null;
        }
        entities.put(entityId, livingEntity);
        return livingEntity;
    }

    private static void renderEntity(GuiGraphics guiGraphics, PreviewLayout layout, LivingEntity entity) {
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraTilt = new Quaternionf().rotateX((float) Math.toRadians(10.0D));
        rotation.mul(cameraTilt);
        LivingRotation original = LivingRotation.capture(entity);
        ScopedState.use(
                () -> {
                    entity.yBodyRot = layout.yawDegrees();
                    entity.setYRot(layout.yawDegrees());
                    entity.setXRot(0.0F);
                    entity.yHeadRot = entity.getYRot();
                    entity.yHeadRotO = entity.getYRot();
                },
                () -> original.restore(entity),
                () -> renderEntityInInventory(guiGraphics, layout, rotation, cameraTilt, entity));
    }

    private static void renderEntityInInventory(GuiGraphics guiGraphics, PreviewLayout layout,
                                                Quaternionf rotation, Quaternionf cameraTilt,
                                                LivingEntity entity) {
        PoseStack pose = guiGraphics.pose();
        ScopedState.use(pose::pushPose, pose::popPose, () -> {
            pose.translate(layout.anchorX(), layout.anchorY(), 50.0D);
            pose.mulPoseMatrix(new Matrix4f().scaling(layout.scale(), layout.scale(), -layout.scale()));
            pose.mulPose(rotation);
            ScopedState.use(Lighting::setupForEntityInInventory, Lighting::setupFor3DItems, () -> {
                EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                Quaternionf originalCamera = new Quaternionf(dispatcher.cameraOrientation());
                ScopedState.use(
                        () -> dispatcher.overrideCameraOrientation(new Quaternionf(cameraTilt).conjugate()),
                        () -> dispatcher.overrideCameraOrientation(originalCamera),
                        () -> {
                            boolean originalShadow = renderShadowEnabled(dispatcher);
                            ScopedState.use(
                                    () -> dispatcher.setRenderShadow(false),
                                    () -> dispatcher.setRenderShadow(originalShadow),
                                    () -> {
                                        try {
                                            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D,
                                                    0.0D, 0.0F, 1.0F, pose,
                                                    guiGraphics.bufferSource(), 0xF000F0));
                                        } finally {
                                            guiGraphics.flush();
                                        }
                                    });
                        });
            });
        });
    }

    private static boolean renderShadowEnabled(EntityRenderDispatcher dispatcher) {
        try {
            return RENDER_SHADOW.getBoolean(dispatcher);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to read entity shadow state", exception);
        }
    }

    private void renderFallback(GuiGraphics guiGraphics, MobIngredient ingredient) {
        guiGraphics.renderItem(new ItemStack(Items.BARRIER), Math.max(0, (size - 16) / 2), 0);
        if (size > 16) {
            String id = ingredient.entityId().toString();
            guiGraphics.drawString(Minecraft.getInstance().font, id, 1, size - 9, 0xFFFFFFFF, true);
        }
    }

    static record PreviewLayout(float scale, float anchorX, float anchorY, float yawDegrees) {

        static PreviewLayout calculate(int size, float width, float height, long animationMillis) {
            float safeWidth = Math.max(0.01F, width);
            float safeHeight = Math.max(0.01F, height);
            float usable = Math.max(1.0F, size - 6.0F);
            float scale = Math.min(usable / safeWidth, usable / safeHeight);
            scale = Math.min(scale, size <= 16 ? 8.0F : 22.0F);
            float scaledHeight = safeHeight * scale;
            double phase = animationMillis * 0.0015D;
            float yaw = 145.0F + (float) Math.sin(phase) * 20.0F;
            return new PreviewLayout(scale, size * 0.5F, (size + scaledHeight) * 0.5F, yaw);
        }
    }

    private record ScissorBounds(int left, int top, int right, int bottom) {

        private static ScissorBounds calculate(Matrix4f pose, int size) {
            Vector3f topLeft = pose.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
            Vector3f bottomRight = pose.transformPosition(size, size, 0.0F, new Vector3f());
            int left = (int) Math.floor(Math.min(topLeft.x(), bottomRight.x()));
            int top = (int) Math.floor(Math.min(topLeft.y(), bottomRight.y()));
            int right = (int) Math.ceil(Math.max(topLeft.x(), bottomRight.x()));
            int bottom = (int) Math.ceil(Math.max(topLeft.y(), bottomRight.y()));
            return new ScissorBounds(left, top, right, bottom);
        }
    }

    private record LivingRotation(float body, float y, float x, float head, float previousHead) {

        private static LivingRotation capture(LivingEntity entity) {
            return new LivingRotation(entity.yBodyRot, entity.getYRot(), entity.getXRot(),
                    entity.yHeadRot, entity.yHeadRotO);
        }

        private void restore(LivingEntity entity) {
            entity.yBodyRot = body;
            entity.setYRot(y);
            entity.setXRot(x);
            entity.yHeadRot = head;
            entity.yHeadRotO = previousHead;
        }
    }

    static final class ScopedState {

        private ScopedState() {
        }

        static void use(Runnable acquire, Runnable release, Runnable action) {
            acquire.run();
            try {
                action.run();
            } finally {
                release.run();
            }
        }
    }

    static final class EntityCache<L, E> implements AutoCloseable {

        private final Consumer<E> discard;
        private final Map<ResourceLocation, E> entities = new HashMap<>();
        private final Set<ResourceLocation> failures = new HashSet<>();
        private L level;

        EntityCache(Consumer<E> discard) {
            this.discard = Objects.requireNonNull(discard, "discard");
        }

        void updateLevel(L currentLevel) {
            if (currentLevel == null || level != currentLevel) {
                discardEntities();
                failures.clear();
                level = currentLevel;
            }
        }

        boolean hasFailed(ResourceLocation entityId) {
            return failures.contains(entityId);
        }

        E get(ResourceLocation entityId) {
            return entities.get(entityId);
        }

        void put(ResourceLocation entityId, E entity) {
            E previous = entities.put(entityId, entity);
            if (previous != null && previous != entity) discard.accept(previous);
        }

        void fail(ResourceLocation entityId) {
            E entity = entities.remove(entityId);
            if (entity != null) discard.accept(entity);
            failures.add(entityId);
        }

        void clear() {
            discardEntities();
            failures.clear();
            level = null;
        }

        @Override
        public void close() {
            clear();
        }

        private void discardEntities() {
            entities.values().forEach(discard);
            entities.clear();
        }
    }
}
