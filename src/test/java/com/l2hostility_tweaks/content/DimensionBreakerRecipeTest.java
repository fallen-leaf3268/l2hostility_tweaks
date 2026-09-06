package com.l2hostility_tweaks.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionBreakerRecipeTest {

    @Test
    void recipeMatchesRequestedPatternIngredientsAndResult() throws IOException {
        JsonObject recipe = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/l2hostility_tweaks/recipes/dimension_breaker.json")))
                .getAsJsonObject();

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("ABA", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("CDC", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("AEA", recipe.getAsJsonArray("pattern").get(2).getAsString());

        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("minecraft:crying_obsidian", key.getAsJsonObject("A").get("item").getAsString());
        assertEquals("l2complements:eternium_pickaxe", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals("l2hostility:miracle_powder", key.getAsJsonObject("C").get("item").getAsString());
        assertEquals("l2hostility:chaos_ingot", key.getAsJsonObject("D").get("item").getAsString());
        assertEquals("l2complements:sculkium_pickaxe", key.getAsJsonObject("E").get("item").getAsString());

        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("l2hostility_tweaks:dimension_breaker", result.get("item").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }
}
