package com.l2hostility_tweaks.compat.jei;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobIngredientRendererTest {

    @Test
    void rectangularRendererUsesIndependentDimensionsAndRejectsNonPositiveBounds() {
        MobIngredientRenderer rectangular = new MobIngredientRenderer(56, 72);
        MobIngredientRenderer square = new MobIngredientRenderer(48);

        assertEquals(56, rectangular.getWidth());
        assertEquals(72, rectangular.getHeight());
        assertEquals(48, square.getWidth());
        assertEquals(48, square.getHeight());
        assertThrows(IllegalArgumentException.class, () -> new MobIngredientRenderer(0, 72));
        assertThrows(IllegalArgumentException.class, () -> new MobIngredientRenderer(56, 0));
    }

    @Test
    void previewLayoutUsesConservativeProjectionEnvelopeAndFixedFootAnchors() {
        List<float[]> boxes = List.of(
                new float[]{0.6F, 1.8F},
                new float[]{1.95F, 2.2F},
                new float[]{2.0F, 2.0F},
                new float[]{0.8F, 6.0F},
                new float[]{80.0F, 40.0F});

        for (int size : List.of(48, 16)) {
            float usable = size == 48 ? 40.0F : 12.0F;
            float maxScale = size == 48 ? 20.0F : 6.0F;
            float anchorY = size == 48 ? 45.0F : 15.0F;
            for (float[] box : boxes) {
                var layout = MobIngredientRenderer.PreviewLayout.calculate(size, size, box[0], box[1]);
                float horizontalEnvelope = box[0] * (float) (Math.sqrt(2.0D) * 1.1D);
                float verticalEnvelope = box[1]
                        + box[0] * (float) (Math.sqrt(2.0D) * Math.sin(Math.toRadians(10.0D)));

                assertTrue(Float.isFinite(layout.scale()));
                assertTrue(layout.scale() > 0.0F);
                assertTrue(layout.scale() <= maxScale);
                assertTrue(horizontalEnvelope * layout.scale() <= usable + 0.001F);
                assertTrue(verticalEnvelope * layout.scale() <= usable + 0.001F);
            }
            assertEquals(size * 0.5F,
                    MobIngredientRenderer.PreviewLayout.calculate(size, size, 1.0F, 2.0F).anchorX());
            assertEquals(anchorY,
                    MobIngredientRenderer.PreviewLayout.calculate(size, size, 1.0F, 2.0F).anchorY());
        }
    }

    @Test
    void rectangularPreviewLayoutUsesIndependentSafeContentBudgetsAndFootAnchor() {
        float width = 2.0F;
        float height = 4.0F;
        var layout = MobIngredientRenderer.PreviewLayout.calculate(56, 72, width, height);
        float horizontalEnvelope = width * (float) (Math.sqrt(2.0D) * 1.1D);
        float verticalEnvelope = height
                + width * (float) (Math.sqrt(2.0D) * Math.sin(Math.toRadians(10.0D)));

        assertTrue(horizontalEnvelope * layout.scale() <= 48.0F + 0.001F);
        assertTrue(verticalEnvelope * layout.scale() <= 64.0F + 0.001F);
        assertEquals(28.0F, layout.anchorX());
        assertEquals(69.0F, layout.anchorY());
    }

    @Test
    void tallRectangularPreviewLayoutKeepsWorstCaseFootProjectionInsideTheViewport() {
        float entityWidth = 10.0F;
        var layout = MobIngredientRenderer.PreviewLayout.calculate(56, 72, entityWidth, 2.0F);
        float downwardProjection = entityWidth * (float) (Math.sqrt(2.0D) * Math.sin(Math.toRadians(10.0D)))
                * layout.scale() * 0.5F;

        assertTrue(downwardProjection <= 72.0F - layout.anchorY() + 0.001F);
    }

    @Test
    void previewLayoutKeepsScaleFiniteAndNonNegativeForEveryPositiveViewportDimension() {
        for (int[] viewport : List.of(
                new int[]{1, 72},
                new int[]{72, 1},
                new int[]{1, 1},
                new int[]{8, 72},
                new int[]{72, 8})) {
            var layout = MobIngredientRenderer.PreviewLayout.calculate(
                    viewport[0], viewport[1], 1.0F, 2.0F);

            assertTrue(Float.isFinite(layout.scale()));
            assertTrue(layout.scale() >= 0.0F);
        }
    }

    @Test
    void lookRotationFacesThePointerAndIsNeutralAtThePreviewCenter() {
        var neutral = MobIngredientRenderer.LookRotation.calculate(0.0, 0.0);
        var left = MobIngredientRenderer.LookRotation.calculate(-40.0, 0.0);
        var right = MobIngredientRenderer.LookRotation.calculate(40.0, 0.0);
        var above = MobIngredientRenderer.LookRotation.calculate(0.0, -40.0);
        var below = MobIngredientRenderer.LookRotation.calculate(0.0, 40.0);

        assertEquals(180.0F, neutral.bodyYawDegrees(), 0.0001F);
        assertEquals(180.0F, neutral.headYawDegrees(), 0.0001F);
        assertEquals(0.0F, neutral.pitchDegrees(), 0.0001F);
        assertTrue(left.headYawDegrees() > 180.0F);
        assertTrue(right.headYawDegrees() < 180.0F);
        assertTrue(above.pitchDegrees() < 0.0F);
        assertTrue(below.pitchDegrees() > 0.0F);
    }

    @Test
    void lookRotationRemainsFiniteAndBoundedForDistantPointers() {
        var rotation = MobIngredientRenderer.LookRotation.calculate(1.0E12, -1.0E12);

        assertTrue(Float.isFinite(rotation.bodyYawDegrees()));
        assertTrue(Float.isFinite(rotation.headYawDegrees()));
        assertTrue(Float.isFinite(rotation.pitchDegrees()));
        assertTrue(rotation.bodyYawDegrees() >= 148.0F && rotation.bodyYawDegrees() <= 212.0F);
        assertTrue(rotation.headYawDegrees() >= 117.0F && rotation.headYawDegrees() <= 243.0F);
        assertTrue(Math.abs(rotation.pitchDegrees()) <= 32.0F);
    }

    @Test
    void scopedStateRestoresNestedGuiStateBeforeRenderFailureEscapes() {
        List<String> events = new ArrayList<>();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                MobIngredientRenderer.ScopedState.use(
                        () -> events.add("push"),
                        () -> events.add("pop"),
                        () -> MobIngredientRenderer.ScopedState.use(
                                () -> events.add("entity-lighting"),
                                () -> events.add("item-lighting"),
                                () -> MobIngredientRenderer.ScopedState.use(
                                        () -> events.add("shadow-off"),
                                        () -> events.add("shadow-on"),
                                        () -> {
                                            events.add("render");
                                            throw new IllegalStateException("render failed");
                                        }))));

        assertEquals("render failed", failure.getMessage());
        assertEquals(List.of("push", "entity-lighting", "shadow-off", "render",
                "shadow-on", "item-lighting", "pop"), events);
    }

    @Test
    void levelChangeDiscardsEntitiesAndInvalidatesFailureCache() {
        MobIngredientRenderer.EntityCache<Object, FakeEntity> cache =
                new MobIngredientRenderer.EntityCache<>(FakeEntity::discard);
        Object firstLevel = new Object();
        FakeEntity zombie = new FakeEntity();
        ResourceLocation zombieId = new ResourceLocation("minecraft:zombie");
        ResourceLocation skeletonId = new ResourceLocation("minecraft:skeleton");

        cache.updateLevel(firstLevel);
        cache.put(zombieId, zombie);
        cache.fail(skeletonId);
        cache.updateLevel(new Object());

        assertEquals(1, zombie.discards);
        assertNull(cache.get(zombieId));
        assertFalse(cache.hasFailed(skeletonId));
    }

    @Test
    void nullLevelAndExplicitCloseDiscardEveryCachedEntity() {
        MobIngredientRenderer.EntityCache<Object, FakeEntity> cache =
                new MobIngredientRenderer.EntityCache<>(FakeEntity::discard);
        FakeEntity zombie = new FakeEntity();
        FakeEntity skeleton = new FakeEntity();

        cache.updateLevel(new Object());
        cache.put(new ResourceLocation("minecraft:zombie"), zombie);
        cache.updateLevel(null);
        cache.put(new ResourceLocation("minecraft:skeleton"), skeleton);
        cache.close();

        assertEquals(1, zombie.discards);
        assertEquals(1, skeleton.discards);
    }

    @Test
    void singleEntityFailureDiscardsOnlyThatEntityAndCachesFailure() {
        MobIngredientRenderer.EntityCache<Object, FakeEntity> cache =
                new MobIngredientRenderer.EntityCache<>(FakeEntity::discard);
        ResourceLocation zombieId = new ResourceLocation("minecraft:zombie");
        ResourceLocation skeletonId = new ResourceLocation("minecraft:skeleton");
        FakeEntity zombie = new FakeEntity();
        FakeEntity skeleton = new FakeEntity();

        cache.updateLevel(new Object());
        cache.put(zombieId, zombie);
        cache.put(skeletonId, skeleton);
        cache.fail(zombieId);

        assertEquals(1, zombie.discards);
        assertEquals(0, skeleton.discards);
        assertNull(cache.get(zombieId));
        assertEquals(skeleton, cache.get(skeletonId));
        assertTrue(cache.hasFailed(zombieId));
    }

    @Test
    void rendererDoesNotDelegateStateSafetyToVanillaInventoryHelper() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/MobIngredientRenderer.java"));

        assertFalse(source.contains("InventoryScreen"));
        assertFalse(source.contains("renderEntityInInventoryFollowsAngle"));
    }

    @Test
    void rendererRestoresThePreviousShadowSetting() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/MobIngredientRenderer.java"));

        assertTrue(source.contains("dispatcher.setRenderShadow(originalShadow)"));
        assertFalse(source.contains("() -> dispatcher.setRenderShadow(true)"));
    }

    @Test
    void fallbackCentersTheBarrierAndLimitsTheIdToTheViewportWidth() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/MobIngredientRenderer.java"));

        assertTrue(source.contains("Math.max(0, (height - 16) / 2)"));
        assertTrue(source.contains("plainSubstrByWidth"));
        assertTrue(source.contains("Math.max(0, width - 2)"));
    }

    private static final class FakeEntity {
        private int discards;

        private void discard() {
            discards++;
        }
    }
}
