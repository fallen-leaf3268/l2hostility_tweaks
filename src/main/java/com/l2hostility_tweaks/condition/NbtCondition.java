package com.l2hostility_tweaks.condition;

import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public class NbtCondition extends SpecialConfigCondition<LivingEntity> {

	private final Map<String, Object> expectedNbt;

	public NbtCondition(Map<String, Object> expectedNbt) {
		super(LivingEntity.class);
		this.id = new ResourceLocation("l2hostility_tweaks", "nbt");
		this.expectedNbt = expectedNbt;
	}

	@Override
	public boolean test(LivingEntity entity) {
		CompoundTag pd = entity.getPersistentData();
		CompoundTag saveData = null;

		for (Map.Entry<String, Object> entry : expectedNbt.entrySet()) {
			if (pd.contains(entry.getKey())) {
				Tag actual = pd.get(entry.getKey());
				if (!tagMatches(actual, entry.getValue())) {
					return false;
				}
				continue;
			}

			if (saveData == null) {
				saveData = entity.saveWithoutId(new CompoundTag());
			}
			if (!saveData.contains(entry.getKey())) {
				return false;
			}
			Tag actual = saveData.get(entry.getKey());
			if (!tagMatches(actual, entry.getValue())) {
				return false;
			}
		}
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
		return false;
	}
}
