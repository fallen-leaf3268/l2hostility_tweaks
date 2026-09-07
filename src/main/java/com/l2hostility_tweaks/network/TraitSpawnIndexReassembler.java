package com.l2hostility_tweaks.network;

import java.util.Optional;

public final class TraitSpawnIndexReassembler {

    private long latestRevision = Long.MIN_VALUE;
    private Pending pending;

    public synchronized Optional<byte[]> accept(TraitSpawnIndexPart part) {
        if (part == null) throw new IllegalArgumentException("Missing trait index part");
        if (part.revision() <= latestRevision) {
            throw new IllegalArgumentException("Old or completed trait index revision: " + part.revision());
        }
        if (pending == null || part.revision() > pending.revision) {
            pending = new Pending(part);
        } else if (part.revision() < pending.revision) {
            throw new IllegalArgumentException("Old trait index revision: " + part.revision());
        } else {
            pending.validateMetadata(part);
        }
        pending.add(part);
        if (!pending.complete()) return Optional.empty();

        byte[] result = pending.join();
        latestRevision = pending.revision;
        pending = null;
        return Optional.of(result);
    }

    public synchronized void reset() {
        latestRevision = Long.MIN_VALUE;
        pending = null;
    }

    private static final class Pending {

        private final long revision;
        private final int partCount;
        private final int totalCompressedBytes;
        private final byte[][] payloads;
        private int received;

        private Pending(TraitSpawnIndexPart first) {
            revision = first.revision();
            partCount = first.partCount();
            totalCompressedBytes = first.totalCompressedBytes();
            payloads = new byte[partCount][];
        }

        private void validateMetadata(TraitSpawnIndexPart part) {
            if (part.partCount() != partCount || part.totalCompressedBytes() != totalCompressedBytes) {
                throw new IllegalArgumentException("Contradictory trait index part metadata");
            }
        }

        private void add(TraitSpawnIndexPart part) {
            if (payloads[part.partIndex()] != null) {
                throw new IllegalArgumentException("Duplicate trait index part: " + part.partIndex());
            }
            payloads[part.partIndex()] = part.payload();
            received++;
        }

        private boolean complete() {
            return received == partCount;
        }

        private byte[] join() {
            byte[] result = new byte[totalCompressedBytes];
            int offset = 0;
            for (byte[] payload : payloads) {
                if (payload == null || payload.length > result.length - offset) {
                    throw new IllegalArgumentException("Missing or oversized trait index part");
                }
                System.arraycopy(payload, 0, result, offset, payload.length);
                offset += payload.length;
            }
            if (offset != totalCompressedBytes) {
                throw new IllegalArgumentException("Incomplete trait index payload");
            }
            return result;
        }
    }
}
