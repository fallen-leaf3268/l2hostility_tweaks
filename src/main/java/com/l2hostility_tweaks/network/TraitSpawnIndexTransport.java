package com.l2hostility_tweaks.network;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public final class TraitSpawnIndexTransport {

    public static final int MAX_PART_PAYLOAD_BYTES = 900 * 1024;
    public static final int MAX_COMPRESSED_BYTES = 32 * 1024 * 1024;
    public static final int MAX_UNCOMPRESSED_NBT_BYTES = 64 * 1024 * 1024;
    public static final int MAX_PART_COUNT =
            (MAX_COMPRESSED_BYTES + MAX_PART_PAYLOAD_BYTES - 1) / MAX_PART_PAYLOAD_BYTES;

    private TraitSpawnIndexTransport() {
    }

    public static List<TraitSpawnIndexPart> encode(TraitSpawnIndexSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        CompoundTag values = TraitSpawnIndexCodec.encode(snapshot);
        validateUncompressedSize(values);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(values, new LimitedOutputStream(output, MAX_COMPRESSED_BYTES));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to compress trait spawn index", exception);
        }
        byte[] compressed = output.toByteArray();
        TraitSpawnIndexSnapshot decoded = decode(compressed);
        if (decoded.revision() != snapshot.revision()) {
            throw new IllegalArgumentException("Encoded trait index revision changed");
        }
        return split(snapshot.revision(), compressed);
    }

    static List<TraitSpawnIndexPart> split(long revision, byte[] compressed) {
        Objects.requireNonNull(compressed, "compressed");
        if (compressed.length <= 0 || compressed.length > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid compressed trait index size: " + compressed.length);
        }
        int count = partCount(compressed.length);
        List<TraitSpawnIndexPart> parts = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int start = index * MAX_PART_PAYLOAD_BYTES;
            int length = Math.min(MAX_PART_PAYLOAD_BYTES, compressed.length - start);
            byte[] payload = new byte[length];
            System.arraycopy(compressed, start, payload, 0, length);
            parts.add(new TraitSpawnIndexPart(revision, index, count, compressed.length, payload));
        }
        return List.copyOf(parts);
    }

    static int partCount(int totalCompressedBytes) {
        if (totalCompressedBytes <= 0 || totalCompressedBytes > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid compressed trait index size: " + totalCompressedBytes);
        }
        return (totalCompressedBytes + MAX_PART_PAYLOAD_BYTES - 1) / MAX_PART_PAYLOAD_BYTES;
    }

    public static TraitSpawnIndexSnapshot decode(byte[] compressed) {
        Objects.requireNonNull(compressed, "compressed");
        if (compressed.length <= 0 || compressed.length > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid compressed trait index size: " + compressed.length);
        }
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(compressed)))) {
            CompoundTag values = NbtIo.read(input, new NbtAccounter(MAX_UNCOMPRESSED_NBT_BYTES));
            return TraitSpawnIndexCodec.decode(values);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Unable to decode trait spawn index", exception);
        }
    }

    public static TraitSpawnIndexSnapshot decode(TraitSpawnIndexReassembler.Completed completed) {
        Objects.requireNonNull(completed, "completed");
        TraitSpawnIndexSnapshot snapshot = decode(completed.compressed());
        if (snapshot.revision() != completed.revision()) {
            throw new IllegalArgumentException("Trait index revision does not match transport envelope");
        }
        return snapshot;
    }

    static OutputStream uncompressedBudget(OutputStream delegate) {
        return new LimitedOutputStream(delegate, MAX_UNCOMPRESSED_NBT_BYTES);
    }

    private static void validateUncompressedSize(CompoundTag values) {
        try (DataOutputStream output = new DataOutputStream(
                uncompressedBudget(OutputStream.nullOutputStream()))) {
            NbtIo.write(values, output);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Uncompressed trait spawn index exceeds limit", exception);
        }
    }

    private static final class LimitedOutputStream extends OutputStream {

        private final OutputStream delegate;
        private final int maximum;
        private int written;

        private LimitedOutputStream(OutputStream delegate, int maximum) {
            this.delegate = delegate;
            this.maximum = maximum;
        }

        @Override
        public void write(int value) throws IOException {
            requireCapacity(1);
            delegate.write(value);
            written++;
        }

        @Override
        public void write(byte[] values, int offset, int length) throws IOException {
            requireCapacity(length);
            delegate.write(values, offset, length);
            written += length;
        }

        private void requireCapacity(int additional) throws IOException {
            if (additional < 0 || additional > maximum - written) {
                throw new IOException("Compressed trait index exceeds limit");
            }
        }
    }
}
