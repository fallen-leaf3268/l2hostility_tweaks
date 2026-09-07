package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.l2hostility_tweaks.network.TraitSpawnIndexCodecTest.completeSnapshot;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSpawnIndexTransportTest {

    @Test
    void compressesChunksReassemblesAndDecodesSnapshot() {
        TraitSpawnIndexSnapshot expected = completeSnapshot(9);
        List<TraitSpawnIndexPart> parts = TraitSpawnIndexTransport.encode(expected);

        assertTrue(TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES < 1024 * 1024);
        assertFalse(parts.isEmpty());
        assertTrue(parts.stream().allMatch(part ->
                part.payload().length <= TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES));

        TraitSpawnIndexReassembler reassembler = new TraitSpawnIndexReassembler();
        byte[] complete = null;
        for (int index = parts.size() - 1; index >= 0; index--) {
            complete = reassembler.accept(parts.get(index)).orElse(complete);
        }

        assertEquals(expected, TraitSpawnIndexTransport.decode(complete));
    }

    @Test
    void waitsForMissingPartAndRejectsDuplicateOldAndContradictoryParts() {
        byte[] bytes = new byte[TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES + 3];
        List<TraitSpawnIndexPart> parts = TraitSpawnIndexTransport.split(5, bytes);
        TraitSpawnIndexReassembler reassembler = new TraitSpawnIndexReassembler();

        assertTrue(reassembler.accept(parts.get(0)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(parts.get(0)));
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(
                new TraitSpawnIndexPart(5, 0, 1, 1, new byte[]{0})));
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(
                new TraitSpawnIndexPart(4, 0, 1, 1, new byte[]{0})));

        assertArrayEquals(bytes, reassembler.accept(parts.get(1)).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(parts.get(1)));
    }

    @Test
    void rejectsOversizedPartMetadataBeforeAllocation() {
        assertThrows(IllegalArgumentException.class, () -> new TraitSpawnIndexPart(
                1, 0, TraitSpawnIndexTransport.MAX_PART_COUNT + 1, 1, new byte[]{0}));
        assertThrows(IllegalArgumentException.class, () -> new TraitSpawnIndexPart(
                1, 0, 1, TraitSpawnIndexTransport.MAX_COMPRESSED_BYTES + 1, new byte[]{0}));
        assertThrows(IllegalArgumentException.class, () -> new TraitSpawnIndexPart(
                1, 0, 1, TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES + 1,
                new byte[TraitSpawnIndexTransport.MAX_PART_PAYLOAD_BYTES + 1]));
    }

    @Test
    void resetAllowsRevisionFromANewConnection() {
        TraitSpawnIndexReassembler reassembler = new TraitSpawnIndexReassembler();
        TraitSpawnIndexPart newer = TraitSpawnIndexTransport.split(9, new byte[]{1}).get(0);
        TraitSpawnIndexPart restarted = TraitSpawnIndexTransport.split(1, new byte[]{2}).get(0);

        reassembler.accept(newer).orElseThrow();
        reassembler.reset();

        assertArrayEquals(new byte[]{2}, reassembler.accept(restarted).orElseThrow());
    }
}
