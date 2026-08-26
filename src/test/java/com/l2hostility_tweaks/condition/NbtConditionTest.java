package com.l2hostility_tweaks.condition;

import com.google.gson.JsonParser;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbtConditionTest {

	@Test
	void recursivelyPartiallyMatchesCompounds() {
		var expected = JsonParser.parseString("{\"ForgeData\":{\"elite\":true}}");

		assertTrue(NbtCondition.matches(expected, compoundWithForgeData(true)));
		assertFalse(NbtCondition.matches(expected, compoundWithForgeData(false)));
		assertFalse(NbtCondition.matches(expected, new CompoundTag()));
	}

	@Test
	void exactlyMatchesListLengthOrderAndContents() {
		var expected = JsonParser.parseString("[1,2,3]");

		assertTrue(NbtCondition.matches(expected, listOf(1, 2, 3)));
		assertFalse(NbtCondition.matches(expected, listOf(1, 3, 2)));
		assertFalse(NbtCondition.matches(expected, listOf(1, 2, 3, 4)));
	}

	@Test
	void preservesDecimalAndLongPrecision() {
		assertTrue(NbtCondition.matches(JsonParser.parseString("0.1"), FloatTag.valueOf(0.1F)));
		assertTrue(NbtCondition.matches(JsonParser.parseString("0.1"), DoubleTag.valueOf(0.1D)));
		assertTrue(NbtCondition.matches(
				JsonParser.parseString("9223372036854775807"), LongTag.valueOf(Long.MAX_VALUE)));
		assertFalse(NbtCondition.matches(
				JsonParser.parseString("9223372036854775806"), LongTag.valueOf(Long.MAX_VALUE)));
		assertFalse(NbtCondition.matches(JsonParser.parseString("20.5"), IntTag.valueOf(20)));
	}

	@Test
	void distinguishesBooleansStringsAndNumbers() {
		assertTrue(NbtCondition.matches(JsonParser.parseString("true"), ByteTag.valueOf(true)));
		assertFalse(NbtCondition.matches(JsonParser.parseString("true"), IntTag.valueOf(1)));
		assertTrue(NbtCondition.matches(JsonParser.parseString("\"elite\""), StringTag.valueOf("elite")));
		assertFalse(NbtCondition.matches(JsonParser.parseString("\"elite\""), StringTag.valueOf("normal")));
	}

	@Test
	void validatesEmptyRootNullAndNestedEmptyObject() {
		assertTrue(NbtCondition.validate(JsonParser.parseString("{}").getAsJsonObject()).isPresent());
		assertTrue(NbtCondition.validate(
				JsonParser.parseString("{\"ForgeData\":null}").getAsJsonObject()).isPresent());
		assertTrue(NbtCondition.validate(
				JsonParser.parseString("{\"ForgeData\":{}}").getAsJsonObject()).isEmpty());

		CompoundTag root = new CompoundTag();
		root.put("ForgeData", new CompoundTag());
		assertTrue(NbtCondition.matches(
				JsonParser.parseString("{\"ForgeData\":{}}"), root));
	}

	@Test
	void rejectsNonFiniteActualNumbers() {
		assertFalse(NbtCondition.matches(JsonParser.parseString("1"), FloatTag.valueOf(Float.NaN)));
		assertFalse(NbtCondition.matches(JsonParser.parseString("1"), DoubleTag.valueOf(Double.POSITIVE_INFINITY)));
	}

	private static CompoundTag compoundWithForgeData(boolean elite) {
		CompoundTag forgeData = new CompoundTag();
		forgeData.putBoolean("elite", elite);
		forgeData.putString("extra", "preserved");
		CompoundTag root = new CompoundTag();
		root.put("ForgeData", forgeData);
		root.putInt("unrelated", 42);
		return root;
	}

	private static ListTag listOf(int... values) {
		ListTag list = new ListTag();
		for (int value : values) {
			list.add(IntTag.valueOf(value));
		}
		return list;
	}
}
