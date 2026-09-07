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
    void previewLayoutFitsTinyNormalWideTallAndHugeEntities() {
        for (float[] box : List.of(
                new float[]{0.2F, 0.3F}, new float[]{0.6F, 1.8F},
                new float[]{4.0F, 1.0F}, new float[]{0.8F, 6.0F},
                new float[]{80.0F, 40.0F})) {
            var layout = MobIngredientRenderer.PreviewLayout.calculate(48, box[0], box[1], 0L);
            assertTrue(Float.isFinite(layout.scale()));
            assertTrue(layout.scale() > 0.0F);
            assertTrue(box[0] * layout.scale() <= 42.01F);
            assertTrue(box[1] * layout.scale() <= 42.01F);
        }
    }

    @Test
    void previewLayoutUsesThreeQuarterViewWithBoundedMotion() {
        for (long millis : List.of(0L, 500L, 1000L, 5000L, 10000L)) {
            float yaw = MobIngredientRenderer.PreviewLayout.calculate(48, 1.0F, 2.0F, millis).yawDegrees();
            assertTrue(yaw >= 125.0F && yaw <= 165.0F);
        }
    }

    @Test
    void previewLayoutKeepsAnimatingAtRealWorldTimestamps() {
        long now = 1_800_000_000_000L;
        float first = MobIngredientRenderer.PreviewLayout.calculate(48, 1.0F, 2.0F, now).yawDegrees();
        float next = MobIngredientRenderer.PreviewLayout.calculate(48, 1.0F, 2.0F, now + 50L).yawDegrees();

        assertTrue(Math.abs(next - first) > 0.0001F);
        assertTrue(Math.abs(next - first) < 2.0F);
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

    private static final class FakeEntity {
        private int discards;

        private void discard() {
            discards++;
        }
    }
}
