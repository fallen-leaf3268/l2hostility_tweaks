package com.l2hostility_tweaks.mixin;

import com.google.gson.*;
import com.l2hostility_tweaks.condition.NbtCondition;
import com.l2hostility_tweaks.util.EntityConfigNbtData;
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
import java.util.*;

@Mixin(value = ConfigMerger.class, remap = false)
public class ConfigMergerMixin {

    private static final Logger LOG = LogManager.getLogger("L2HostilityFix/ConfigMerger");
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, Map<Integer, NbtEntry>> nbtStore;
    private static volatile boolean nbtStoreBuilt;

    private record NbtEntry(EntityConfigNbtData.State state, JsonObject condition) {
    }

    @Inject(method = "merge", at = @At("HEAD"))
    private void l2fix$onBeforeMerge(List<BaseConfig> list, CallbackInfoReturnable<Object> cir) {
        nbtStoreBuilt = false;
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            // Build NBT store lazily using listResources (same method L2Serial uses)
            if (!nbtStoreBuilt) {
                nbtStore = l2fix$buildNbtStore(server);
                nbtStoreBuilt = true;
            }
            if (nbtStore == null || nbtStore.isEmpty()) return;

            int nbtSet = 0;
            for (BaseConfig baseConfig : list) {
                if (!(baseConfig instanceof EntityConfig ec)) continue;
                if (ec.getID() == null) continue;

                Map<Integer, NbtEntry> indexMap = nbtStore.get(ec.getID());
                if (indexMap == null) continue;

                for (Map.Entry<Integer, NbtEntry> entry : indexMap.entrySet()) {
                    int i = entry.getKey();
                    if (i < ec.list.size()) {
                        NbtEntry nbtEntry = entry.getValue();
                        ((EntityConfigNbtData) (Object) ec.list.get(i))
                                .l2fix$setNbtCondition(nbtEntry.state(), nbtEntry.condition());
                        nbtSet++;
                        LOG.info("[ConfigMergerMixin] Set NBT state {} on config[{}] from '{}'",
                                nbtEntry.state(), i, ec.getID());
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

    private static Map<ResourceLocation, Map<Integer, NbtEntry>> l2fix$buildNbtStore(MinecraftServer server) {
        Map<ResourceLocation, Map<Integer, NbtEntry>> store = new LinkedHashMap<>();
        int totalFound = 0;

        // Use listResources — exactly how SimpleJsonResourceReloadListener finds JSON files
        var resourceManager = server.getResourceManager();
        if (resourceManager == null) return new LinkedHashMap<>();
        Map<ResourceLocation, Resource> found = resourceManager
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
                    JsonElement configElement = listArray.get(i);
                    if (!configElement.isJsonObject()) continue;
                    JsonElement nbtElement = configElement.getAsJsonObject().get("nbt");
                    if (nbtElement == null) continue;

                    NbtEntry nbtEntry;
                    if (!nbtElement.isJsonObject()) {
                        nbtEntry = new NbtEntry(EntityConfigNbtData.State.INVALID, null);
                        LOG.warn("[ConfigMergerMixin] Invalid NBT condition in {}[{}]: nbt must be an object",
                                configId, i);
                    } else {
                        JsonObject condition = nbtElement.getAsJsonObject();
                        Optional<String> error = NbtCondition.validate(condition);
                        if (error.isPresent()) {
                            nbtEntry = new NbtEntry(EntityConfigNbtData.State.INVALID, null);
                            LOG.warn("[ConfigMergerMixin] Invalid NBT condition in {}[{}]: {}",
                                    configId, i, error.get());
                        } else {
                            nbtEntry = new NbtEntry(EntityConfigNbtData.State.VALID, condition.deepCopy());
                        }
                    }

                    store.computeIfAbsent(configId, k -> new LinkedHashMap<>()).put(i, nbtEntry);
                    totalFound++;
                    LOG.info("[ConfigMergerMixin] Found NBT state {} in {}[{}]",
                            nbtEntry.state(), configId, i);
                }
            } catch (Exception exception) {
                LOG.warn("[ConfigMergerMixin] Failed to inspect entity config resource {}", id, exception);
            }
        }

        LOG.info("[ConfigMergerMixin] NBT store built: {} entries from {} files", totalFound, store.size());
        return store;
    }

}
