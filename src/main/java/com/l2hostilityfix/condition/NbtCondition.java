package com.l2hostilityfix.condition;

import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class NbtCondition extends SpecialConfigCondition<LivingEntity> {

    private static final Logger LOG = LogManager.getLogger("L2HostilityFix/NbtCondition");

    private final Map<String, Object> expectedNbt;

    public NbtCondition(Map<String, Object> expectedNbt) {
        super(LivingEntity.class);
        this.id = new ResourceLocation("l2hostilityfix", "nbt");
        this.expectedNbt = expectedNbt;
    }

    @Override
    public boolean test(LivingEntity entity) {
        // Check entity's full save data (includes top-level tags like isApollyon)
        // as well as ForgeData persistentData.
        CompoundTag data = entity.saveWithoutId(new CompoundTag());

        for (Map.Entry<String, Object> entry : expectedNbt.entrySet()) {
            if (!data.contains(entry.getKey())) {
                // Also try Forge persistentData as fallback
                CompoundTag pd = entity.getPersistentData();
                if (pd.contains(entry.getKey())) {
                    Tag actual = pd.get(entry.getKey());
                    if (!tagMatches(actual, entry.getValue())) {
                        LOG.info("[NbtCondition] {} FAIL: key '{}' mismatch in pd - expected={} ({}), actual={} ({})",
                                entity.getName().getString(), entry.getKey(),
                                entry.getValue(), entry.getValue().getClass().getSimpleName(),
                                actual, actual.getClass().getSimpleName());
                        return false;
                    }
                } else {
                    LOG.info("[NbtCondition] {} FAIL: missing key '{}'. Full NBT keys: [{}]",
                            entity.getName().getString(), entry.getKey(),
                            String.join(", ", data.getAllKeys()));
                    return false;
                }
            } else {
                Tag actual = data.get(entry.getKey());
                if (!tagMatches(actual, entry.getValue())) {
                    LOG.info("[NbtCondition] {} FAIL: key '{}' mismatch - expected={} ({}), actual={} ({}). Full NBT: {}",
                            entity.getName().getString(), entry.getKey(),
                            entry.getValue(), entry.getValue().getClass().getSimpleName(),
                            actual, actual.getClass().getSimpleName(), data);
                    return false;
                }
            }
        }
        LOG.info("[NbtCondition] MATCHED for {}: {}", entity.getName().getString(), expectedNbt);
        return true;
    }

    private static boolean tagMatches(Tag actual, Object expected) {
        if (expected instanceof Boolean b) {
            return actual.getId() == Tag.TAG_BYTE
                    && ((net.minecraft.nbt.ByteTag) actual).getAsByte() == (b ? (byte) 1 : (byte) 0);
        }
        if (expected instanceof Number n) {
            return actual instanceof net.minecraft.nbt.NumericTag num
                    && num.getAsInt() == n.intValue();
        }
        if (expected instanceof String s) {
            return actual.getAsString().equals(s);
        }
        return actual.getAsString().equals(String.valueOf(expected));
    }
}
