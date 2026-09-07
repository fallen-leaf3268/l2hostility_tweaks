package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
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
        TraitSpawnIndexReassembler.Completed complete = null;
        for (int index = parts.size() - 1; index >= 0; index--) {
            complete = reassembler.accept(parts.get(index)).orElse(complete);
        }

        assertEquals(expected, TraitSpawnIndexTransport.decode(complete));
        assertTrue(reassembler.commit(complete));
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

        TraitSpawnIndexReassembler.Completed complete = reassembler.accept(parts.get(1)).orElseThrow();
        assertEquals(5, complete.revision());
        assertArrayEquals(bytes, complete.compressed());
        assertTrue(reassembler.commit(complete));
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

        assertTrue(reassembler.commit(reassembler.accept(newer).orElseThrow()));
        reassembler.reset();

        TraitSpawnIndexReassembler.Completed complete = reassembler.accept(restarted).orElseThrow();
        assertArrayEquals(new byte[]{2}, complete.compressed());
    }

    @Test
    void corruptedPayloadDoesNotConsumeRevisionAndSameRevisionCanRetry() {
        TraitSpawnIndexReassembler reassembler = new TraitSpawnIndexReassembler();
        TraitSpawnIndexPart corrupted = TraitSpawnIndexTransport.split(7, new byte[]{1}).get(0);

        TraitSpawnIndexReassembler.Completed rejected = reassembler.accept(corrupted).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> TraitSpawnIndexTransport.decode(rejected));

        TraitSpawnIndexReassembler.Completed retried = acceptAll(
                reassembler, TraitSpawnIndexTransport.encode(completeSnapshot(7)));

        assertEquals(7, rejected.revision());
        assertEquals(completeSnapshot(7), TraitSpawnIndexTransport.decode(retried));
        assertTrue(reassembler.commit(retried));
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(corrupted));
    }

    @Test
    void decodedRevisionMustMatchEnvelopeAndMismatchCanRetry() {
        List<TraitSpawnIndexPart> encoded = TraitSpawnIndexTransport.encode(completeSnapshot(8));
        byte[] compressed = join(encoded);
        TraitSpawnIndexReassembler reassembler = new TraitSpawnIndexReassembler();
        TraitSpawnIndexPart contradictory = TraitSpawnIndexTransport.split(7, compressed).get(0);

        TraitSpawnIndexReassembler.Completed first = reassembler.accept(contradictory).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> TraitSpawnIndexTransport.decode(first));

        TraitSpawnIndexReassembler.Completed retry = reassembler.accept(contradictory).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> TraitSpawnIndexTransport.decode(retry));
    }

    @Test
    void senderUncompressedBudgetRejectsHighlyCompressibleDataWithoutBuffering() throws Exception {
        byte[] repeated = new byte[8192];
        OutputStream output = TraitSpawnIndexTransport.uncompressedBudget(OutputStream.nullOutputStream());
        int writes = TraitSpawnIndexTransport.MAX_UNCOMPRESSED_NBT_BYTES / repeated.length;

        for (int index = 0; index < writes; index++) output.write(repeated);

        assertThrows(IOException.class, () -> output.write(0));
    }

    private static byte[] join(List<TraitSpawnIndexPart> parts) {
        int size = parts.stream().mapToInt(part -> part.payload().length).sum();
        byte[] joined = new byte[size];
        int offset = 0;
        for (TraitSpawnIndexPart part : parts) {
            byte[] payload = part.payload();
            System.arraycopy(payload, 0, joined, offset, payload.length);
            offset += payload.length;
        }
        return joined;
    }

    private static TraitSpawnIndexReassembler.Completed acceptAll(
            TraitSpawnIndexReassembler reassembler, List<TraitSpawnIndexPart> parts) {
        TraitSpawnIndexReassembler.Completed complete = null;
        for (TraitSpawnIndexPart part : parts) {
            complete = reassembler.accept(part).orElse(complete);
        }
        if (complete == null) throw new IllegalStateException("Incomplete test payload");
        return complete;
    }
}
