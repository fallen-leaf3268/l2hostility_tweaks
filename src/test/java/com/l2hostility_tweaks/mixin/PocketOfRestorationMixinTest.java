package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketOfRestorationMixinTest {

    @Test
    void completesRestoreWhenSharedFallbackDeliversTheItem() {
        AtomicBoolean delivered = new AtomicBoolean();

        boolean restored = PocketOfRestorationMixin.l2fix$restoreStoredItem(
                false,
                () -> {},
                () -> {
                    delivered.set(true);
                    return true;
                });

        assertTrue(restored);
        assertTrue(delivered.get());
    }

    @Test
    void keepsStoredItemWhenSharedFallbackCannotDeliverIt() {
        boolean restored = PocketOfRestorationMixin.l2fix$restoreStoredItem(
                false,
                () -> {},
                () -> false);

        assertFalse(restored);
    }

    @Test
    void rejectsMalformedStoredItemData() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SealedItem.DATA, "corrupt");

        assertFalse(PocketOfRestorationMixin.l2fix$hasStoredItemData(tag));
    }

    @Test
    void multiSlotPathUsesExplicitLevelsWithoutGluttonyThreadLocal() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixin.java"));

        assertFalse(source.contains("ThreadLocal<Integer> l2fix$gluttonyLevel"));
        assertTrue(source.contains("l2fix$runMultiSlotTick(slotContext, stack, abyss, gluttony)"));
        assertTrue(source.contains("l2fix$runMultiSlotTick(SlotContext slotContext, ItemStack stack, int abyss, int gluttony)"));
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
}
