package com.l2hostilityfix.mixin;

import com.google.gson.*;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2library.serial.config.BaseConfig;
import dev.xkmc.l2library.serial.config.ConfigMerger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;

@Mixin(value = ConfigMerger.class, remap = false)
public class ConfigMergerMixin {

    private static final Logger LOG = LogManager.getLogger("L2HostilityFix/ConfigMerger");
    private static final Gson GSON = new Gson();
    private static volatile Field NBT_FIELD;
    private static volatile boolean NBT_FIELD_LOOKED_UP;
    private static volatile Map<ResourceLocation, Map<Integer, Map<String, Object>>> nbtStore;
    private static volatile boolean nbtStoreBuilt;

    @Inject(method = "merge", at = @At("HEAD"))
    private void onBeforeMerge(List<BaseConfig> list, CallbackInfoReturnable<Object> cir) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            // Build NBT store lazily using listResources (same method L2Serial uses)
            if (!nbtStoreBuilt) {
                nbtStore = buildNbtStore(server);
                nbtStoreBuilt = true;
            }
            if (nbtStore == null || nbtStore.isEmpty()) return;

            int nbtSet = 0;
            for (BaseConfig baseConfig : list) {
                if (!(baseConfig instanceof EntityConfig ec)) continue;
                if (ec.getID() == null) continue;

                Map<Integer, Map<String, Object>> indexMap = nbtStore.get(ec.getID());
                if (indexMap == null) continue;

                for (Map.Entry<Integer, Map<String, Object>> nbtEntry : indexMap.entrySet()) {
                    int i = nbtEntry.getKey();
                    if (i < ec.list.size()) {
                        setNbt(ec.list.get(i), nbtEntry.getValue());
                        nbtSet++;
                        LOG.info("[ConfigMergerMixin] Set NBT on config[{}] from '{}': {}",
                                i, ec.getID(), nbtEntry.getValue());
                    }
                }
            }

            if (nbtSet > 0) {
                LOG.info("[ConfigMergerMixin] Total NBT entries injected: {}", nbtSet);
            }

        } catch (Exception e) {
            LOG.error("[ConfigMergerMixin] Failed to inject NBT data", e);
        }
    }

    private static Map<ResourceLocation, Map<Integer, Map<String, Object>>> buildNbtStore(MinecraftServer server) {
        Map<ResourceLocation, Map<Integer, Map<String, Object>>> store = new LinkedHashMap<>();
        int totalFound = 0;

        // Use listResources — exactly how SimpleJsonResourceReloadListener finds JSON files
        Map<ResourceLocation, Resource> found = server.getResourceManager()
                .listResources("l2hostility_config/entity", path -> path.getPath().endsWith(".json"));

        LOG.info("[ConfigMergerMixin] listResources found {} entity config files", found.size());

        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation id = entry.getKey();

            // Convert listResources key to L2Serial config ID:
            // "goety:l2hostility_config/entity/apo.json" → "goety:apo"
            String path = id.getPath();
            String stripped = path.substring("l2hostility_config/entity/".length());
            if (stripped.endsWith(".json")) stripped = stripped.substring(0, stripped.length() - 5);
            ResourceLocation configId = new ResourceLocation(id.getNamespace(), stripped);

            try (var reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                JsonArray listArray = root.getAsJsonArray("list");
                if (listArray == null) continue;

                for (int i = 0; i < listArray.size(); i++) {
                    JsonObject entryObj = listArray.get(i).getAsJsonObject();
                    JsonObject nbtObj = entryObj.getAsJsonObject("nbt");
                    if (nbtObj == null) continue;

                    Map<String, Object> nbtMap = parseNbt(nbtObj);
                    if (nbtMap.isEmpty()) continue;

                    store.computeIfAbsent(configId, k -> new LinkedHashMap<>()).put(i, nbtMap);
                    totalFound++;
                    LOG.info("[ConfigMergerMixin] Found NBT in {}[{}]: {}", configId, i, nbtMap);
                }
            } catch (Exception ignored) {
            }
        }

        LOG.info("[ConfigMergerMixin] NBT store built: {} entries from {} files", totalFound, store.size());
        return store;
    }

    private static Map<String, Object> parseNbt(JsonObject nbtObj) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> kv : nbtObj.entrySet()) {
            JsonElement val = kv.getValue();
            if (val instanceof JsonPrimitive p) {
                if (p.isBoolean()) map.put(kv.getKey(), p.getAsBoolean());
                else if (p.isNumber()) map.put(kv.getKey(), p.getAsInt());
                else map.put(kv.getKey(), p.getAsString());
            }
        }
        return map;
    }

    private static void setNbt(EntityConfig.Config config, Map<String, Object> nbt) {
        if (!NBT_FIELD_LOOKED_UP) {
            try {
                NBT_FIELD = EntityConfig.Config.class.getField("nbt");
                LOG.info("[ConfigMergerMixin] Found 'nbt' field via reflection: {}", NBT_FIELD);
            } catch (NoSuchFieldException e) {
                LOG.error("[ConfigMergerMixin] Could not find 'nbt' field on Config class!", e);
            }
            NBT_FIELD_LOOKED_UP = true;
        }
        if (NBT_FIELD == null) return;
        try {
            NBT_FIELD.set(config, nbt);
        } catch (IllegalAccessException e) {
            LOG.error("[ConfigMergerMixin] Failed to set nbt field", e);
        }
    }
}
