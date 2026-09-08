package com.l2hostility_tweaks.compat.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.ingredients.IIngredientRenderer;
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

    private final int width;
    private final int height;
    private final MobIngredientHelper helper = new MobIngredientHelper();
    private final EntityCache<Level, LivingEntity> entities = new EntityCache<>(Entity::discard);
    private double pointerOffsetX;
    private double pointerOffsetY;

    public MobIngredientRenderer(int size) {
        this(size, size);
    }

    public MobIngredientRenderer(int width, int height) {
        if (width < 1 || height < 1) throw new IllegalArgumentException("width and height must be positive");
        this.width = width;
        this.height = height;
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
            PreviewLayout layout = PreviewLayout.calculate(width, height, entity.getBbWidth(), entity.getBbHeight());
            LookRotation look = LookRotation.calculate(pointerOffsetX, pointerOffsetY);
            ScissorBounds scissor = ScissorBounds.calculate(guiGraphics.pose().last().pose(), width, height);
            ScopedState.use(
                    () -> guiGraphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom()),
                    guiGraphics::disableScissor,
                    () -> renderEntity(guiGraphics, layout, look, entity));
        } catch (RuntimeException | LinkageError exception) {
            entities.fail(entityId);
            renderFallback(guiGraphics, ingredient);
        }
    }

    @Override
    public List<Component> getTooltip(MobIngredient ingredient, TooltipFlag tooltipFlag) {
        String displayName = helper.getDisplayName(ingredient);
        return List.of(Component.literal(displayName));
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void clear() {
        entities.clear();
    }

    public void setMousePosition(double pointerOffsetX, double pointerOffsetY) {
        this.pointerOffsetX = pointerOffsetX;
        this.pointerOffsetY = pointerOffsetY;
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

    private static void renderEntity(GuiGraphics guiGraphics, PreviewLayout layout, LookRotation look,
                                     LivingEntity entity) {
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraTilt = new Quaternionf().rotateX((float) Math.toRadians(10.0D));
        rotation.mul(cameraTilt);
        LivingRotation original = LivingRotation.capture(entity);
        ScopedState.use(
                () -> {
                    entity.yBodyRot = look.bodyYawDegrees();
                    entity.setYRot(look.headYawDegrees());
                    entity.setXRot(look.pitchDegrees());
                    entity.yHeadRot = look.headYawDegrees();
                    entity.yHeadRotO = look.headYawDegrees();
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
            return RenderShadowField.INSTANCE.getBoolean(dispatcher);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to read entity shadow state", exception);
        }
    }

    private static final class RenderShadowField {

        private static final Field INSTANCE = ObfuscationReflectionHelper.findField(
                EntityRenderDispatcher.class, "f_114368_");
    }

    private void renderFallback(GuiGraphics guiGraphics, MobIngredient ingredient) {
        guiGraphics.renderItem(new ItemStack(Items.BARRIER), Math.max(0, (width - 16) / 2),
                Math.max(0, (height - 16) / 2));
        if (height > 16) {
            String id = Minecraft.getInstance().font.plainSubstrByWidth(ingredient.entityId().toString(),
                    Math.max(0, width - 2));
            guiGraphics.drawString(Minecraft.getInstance().font, id, 1, height - 9, 0xFFFFFFFF, false);
        }
    }

    static record PreviewLayout(float scale, float anchorX, float anchorY) {

        static PreviewLayout calculate(int viewportWidth, int viewportHeight, float width, float height) {
            float safeWidth = Math.max(0.01F, width);
            float safeHeight = Math.max(0.01F, height);
            boolean compact = viewportWidth <= 16 && viewportHeight <= 16;
            float usableWidth = compact ? 12.0F : Math.max(0.0F, viewportWidth - 8.0F);
            float usableHeight = compact ? 12.0F : Math.max(0.0F, viewportHeight - 8.0F);
            float maxScale = compact ? 6.0F : 20.0F;
            float horizontalEnvelope = safeWidth * (float) (Math.sqrt(2.0D) * 1.1D);
            float verticalEnvelope = safeHeight
                    + safeWidth * (float) (Math.sqrt(2.0D) * Math.sin(Math.toRadians(10.0D)));
            float scale = Math.min(usableWidth / horizontalEnvelope, usableHeight / verticalEnvelope);
            scale = Math.min(scale, maxScale);
            if (viewportHeight > viewportWidth) {
                float downwardProjection = safeWidth
                        * (float) (Math.sqrt(2.0D) * Math.sin(Math.toRadians(10.0D))) * 0.5F;
                scale = Math.min(scale, (compact ? 1.0F : 3.0F) / downwardProjection);
            }
            return new PreviewLayout(scale, viewportWidth * 0.5F, viewportHeight - (compact ? 1.0F : 3.0F));
        }
    }

    static record LookRotation(float bodyYawDegrees, float headYawDegrees, float pitchDegrees) {

        static LookRotation calculate(double pointerOffsetX, double pointerOffsetY) {
            float horizontal = (float) Math.atan(pointerOffsetX / 40.0D);
            float vertical = (float) Math.atan(pointerOffsetY / 40.0D);
            return new LookRotation(180.0F - horizontal * 20.0F,
                    180.0F - horizontal * 40.0F, vertical * 20.0F);
        }
    }

    private record ScissorBounds(int left, int top, int right, int bottom) {

        private static ScissorBounds calculate(Matrix4f pose, int width, int height) {
            Vector3f topLeft = pose.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
            Vector3f bottomRight = pose.transformPosition(width, height, 0.0F, new Vector3f());
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
