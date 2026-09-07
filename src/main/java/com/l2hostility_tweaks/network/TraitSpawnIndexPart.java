package com.l2hostility_tweaks.network;

import java.util.Arrays;

public record TraitSpawnIndexPart(long revision, int partIndex, int partCount,
                                  int totalCompressedBytes, byte[] payload) {

    public TraitSpawnIndexPart {
        if (partCount <= 0 || partCount > TraitSpawnIndexTransport.MAX_PART_COUNT) {
            throw new IllegalArgumentException("Invalid trait index part count: " + partCount);
        }
        if (partIndex < 0 || partIndex >= partCount) {
            throw new IllegalArgumentException("Invalid trait index part index: " + partIndex);
        }
        if (totalCompressedBytes <= 0 || totalCompressedBytes > TraitSpawnIndexTransport.MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid compressed trait index size: " + totalCompressedBytes);
        }
        int expectedCount = TraitSpawnIndexTransport.partCount(totalCompressedBytes);
        if (partCount != expectedCount) {
            throw new IllegalArgumentException("Contradictory trait index part count");
        }
        int expectedPayloadBytes = partIndex == partCount - 1
                ? totalCompressedBytes - partIndex * TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES
                : TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES;
        if (payload == null || payload.length != expectedPayloadBytes) {
            throw new IllegalArgumentException("Invalid trait index part payload size");
        }
        payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
