package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketOfRestorationMixinTest {

    @Test
    void completesRestoreWhenSharedFallbackDeliversTheItem() {
        AtomicBoolean delivered = new AtomicBoolean();

        boolean restored = MixinTestInvoker.call(
                PocketOfRestorationMixin.class, "l2fix$restoreStoredItem",
                false,
                (Runnable) () -> {},
                (java.util.function.BooleanSupplier) () -> {
                    delivered.set(true);
                    return true;
                });

        assertTrue(restored);
        assertTrue(delivered.get());
    }

    @Test
    void keepsStoredItemWhenSharedFallbackCannotDeliverIt() {
        boolean restored = MixinTestInvoker.call(
                PocketOfRestorationMixin.class, "l2fix$restoreStoredItem",
                false,
                (Runnable) () -> {},
                (java.util.function.BooleanSupplier) () -> false);

        assertFalse(restored);
    }

    @Test
    void rejectsMalformedStoredItemData() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SealedItem.DATA, "corrupt");

        assertFalse(MixinTestInvoker.<Boolean>call(
                PocketOfRestorationMixin.class, "l2fix$hasStoredItemData", tag));
    }

    @Test
    void existingExtraSlotsRemainRestorableAfterGluttonyDowngrade() {
        CompoundTag pocket = new CompoundTag();
        pocket.put("UnsealRoot_3", new CompoundTag());

        assertEquals(List.of(0, 3),
                MixinTestInvoker.call(PocketOfRestorationMixin.class,
                        "l2fix$restorableSlotIndices", pocket, 1));
    }

    @Test
    void multiSlotPathUsesExplicitLevelsWithoutGluttonyThreadLocal() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixin.java"));

        assertFalse(source.contains("ThreadLocal<Integer> l2fix$gluttonyLevel"));
        assertTrue(source.contains("l2fix$runMultiSlotTick(slotContext, stack, abyss, activeSlots, restoreSlots)"));
        assertTrue(source.contains("int activeSlots, List<Integer> restoreSlots)"));
    }

    @Test
    void captureClearsStaleAbyssBeforeAnyEarlyReturn() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixin.java"));
        int capture = source.indexOf("private void l2fix$captureAndRoute");
        int clientReturn = source.indexOf("if (le.level().isClientSide) return;", capture);
        int clear = source.indexOf("l2fix$abyssLevel.remove();", capture);

        assertTrue(capture >= 0);
        assertTrue(clear > capture && clear < clientReturn);
    }

    @Test
    void normalPocketOperationsOnlyUseDebugLogging() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixin.java"));

        assertFalse(source.contains("LOGGER.info("));
        assertTrue(source.contains("LOGGER.debug(\"speedUp original={} reduced={} level={}\""));
        assertTrue(source.contains("LOGGER.debug(\"sync forced for slot {}\""));
    }
}
