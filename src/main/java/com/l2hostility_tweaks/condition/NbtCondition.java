package com.l2hostility_tweaks.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class NbtCondition extends SpecialConfigCondition<LivingEntity> {

	private final JsonObject expectedNbt;

	public NbtCondition(JsonObject expectedNbt) {
		super(LivingEntity.class);
		this.id = new ResourceLocation("l2hostility_tweaks", "nbt");
		this.expectedNbt = expectedNbt;
	}

	@Override
	public boolean test(LivingEntity entity) {
		CompoundTag pd = entity.getPersistentData();
		CompoundTag saveData = null;

		for (Map.Entry<String, JsonElement> entry : expectedNbt.entrySet()) {
			if (pd.contains(entry.getKey())) {
				Tag actual = pd.get(entry.getKey());
				if (!matches(entry.getValue(), actual)) {
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
			if (!matches(entry.getValue(), actual)) {
				return false;
			}
		}
		return true;
	}

	public static Optional<String> validate(JsonObject expected) {
		if (expected == null) {
			return Optional.of("NBT condition must be an object");
		}
		if (expected.size() == 0) {
			return Optional.of("NBT condition must not be empty");
		}
		return validateElement(expected, "nbt");
	}

	private static Optional<String> validateElement(JsonElement expected, String path) {
		if (expected == null || expected.isJsonNull()) {
			return Optional.of(path + " must not be null");
		}
		if (expected.isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : expected.getAsJsonObject().entrySet()) {
				Optional<String> error = validateElement(entry.getValue(), path + "." + entry.getKey());
				if (error.isPresent()) return error;
			}
			return Optional.empty();
		}
		if (expected.isJsonArray()) {
			for (int i = 0; i < expected.getAsJsonArray().size(); i++) {
				Optional<String> error = validateElement(expected.getAsJsonArray().get(i), path + "[" + i + "]");
				if (error.isPresent()) return error;
			}
			return Optional.empty();
		}
		JsonPrimitive primitive = expected.getAsJsonPrimitive();
		if (primitive.isBoolean() || primitive.isString()) {
			return Optional.empty();
		}
		if (primitive.isNumber()) {
			try {
				new BigDecimal(primitive.getAsString());
				return Optional.empty();
			} catch (NumberFormatException exception) {
				return Optional.of(path + " contains an invalid number");
			}
		}
		return Optional.of(path + " contains an unsupported value");
	}

	static boolean matches(JsonElement expected, Tag actual) {
		if (expected == null || expected.isJsonNull() || actual == null) {
			return false;
		}
		if (expected.isJsonObject()) {
			if (!(actual instanceof CompoundTag compound)) return false;
			for (Map.Entry<String, JsonElement> entry : expected.getAsJsonObject().entrySet()) {
				if (!compound.contains(entry.getKey()) || !matches(entry.getValue(), compound.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		}
		if (expected.isJsonArray()) {
			if (!(actual instanceof ListTag list) || list.size() != expected.getAsJsonArray().size()) return false;
			for (int i = 0; i < list.size(); i++) {
				if (!matches(expected.getAsJsonArray().get(i), list.get(i))) return false;
			}
			return true;
		}
		JsonPrimitive primitive = expected.getAsJsonPrimitive();
		if (primitive.isBoolean()) {
			return actual instanceof ByteTag byteTag
					&& byteTag.getAsByte() == (primitive.getAsBoolean() ? (byte) 1 : (byte) 0);
		}
		if (primitive.isString()) {
			return actual instanceof StringTag && actual.getAsString().equals(primitive.getAsString());
		}
		if (primitive.isNumber() && actual instanceof NumericTag numeric) {
			try {
				BigDecimal expectedNumber = new BigDecimal(primitive.getAsString());
				BigDecimal actualNumber;
				if (actual instanceof FloatTag) {
					float value = numeric.getAsFloat();
					if (!Float.isFinite(value)) return false;
					actualNumber = new BigDecimal(Float.toString(value));
				} else if (actual instanceof DoubleTag) {
					double value = numeric.getAsDouble();
					if (!Double.isFinite(value)) return false;
					actualNumber = BigDecimal.valueOf(value);
				} else {
					actualNumber = BigDecimal.valueOf(numeric.getAsLong());
				}
				return expectedNumber.compareTo(actualNumber) == 0;
			} catch (NumberFormatException exception) {
				return false;
			}
		}
		return false;
	}
}
