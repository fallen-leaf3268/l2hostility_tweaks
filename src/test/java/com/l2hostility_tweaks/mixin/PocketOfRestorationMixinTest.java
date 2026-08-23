package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

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
}
