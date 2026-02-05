package minidisc;

import java.io.IOException;
import java.util.Objects;

/**
 * Cluster-range validation lives here.
 * Sector structure validation lives in MiniDiscSector factories.
 */
public final class LayoutCheckedMiniDiscImage implements MiniDiscImage {
    private final MiniDiscImage delegate;

    private final int leadInStartClusterInclusive;   // typically 0
    private final int leadInEndClusterExclusive;     // typically 3
    private final int utocStartClusterInclusive;     // typically 3
    private final int utocEndClusterExclusive;       // typically 0x32
    private final int programStartClusterInclusive;  // typically 0x32
    private final int leadOutStartClusterExclusive;  // 0x0715 (MD60) or 0x08CC (MD74)
    private final int leadOutClusters;               // typically 115

    private LayoutCheckedMiniDiscImage(Builder b) {
        this.delegate = b.delegate;
        this.leadInStartClusterInclusive = b.leadInStartClusterInclusive;
        this.leadInEndClusterExclusive = b.leadInEndClusterExclusive;
        this.utocStartClusterInclusive = b.utocStartClusterInclusive;
        this.utocEndClusterExclusive = b.utocEndClusterExclusive;
        this.programStartClusterInclusive = b.programStartClusterInclusive;
        this.leadOutStartClusterExclusive = b.leadOutStartClusterExclusive;
        this.leadOutClusters = b.leadOutClusters;
    }

    public static final class Builder {
        private final MiniDiscImage delegate;
        private Integer leadInStartClusterInclusive;
        private Integer leadInEndClusterExclusive;
        private Integer utocStartClusterInclusive;
        private Integer utocEndClusterExclusive;
        private Integer programStartClusterInclusive;
        private Integer leadOutStartClusterExclusive;
        private Integer leadOutClusters;

        public Builder(MiniDiscImage delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public Builder withLeadInStartClusterInclusive(int v) {
            this.leadInStartClusterInclusive = v;
            return this;
        }

        public Builder withLeadInEndClusterExclusive(int v) {
            this.leadInEndClusterExclusive = v;
            return this;
        }

        public Builder withUtocStartClusterInclusive(int v) {
            this.utocStartClusterInclusive = v;
            return this;
        }

        public Builder withProgramStartClusterInclusive(int v) {
            this.programStartClusterInclusive = v;
            return this;
        }

        public Builder withLeadOutStartClusterExclusive(int v) {
            this.leadOutStartClusterExclusive = v;
            return this;
        }

        public Builder withLeadOutClusters(int v) {
            this.leadOutClusters = v;
            return this;
        }

        public LayoutCheckedMiniDiscImage build() {
            // présence de tous les paramètres
            if (leadInStartClusterInclusive == null ||
                    leadInEndClusterExclusive == null ||
                    utocStartClusterInclusive == null ||
                    programStartClusterInclusive == null ||
                    leadOutStartClusterExclusive == null ||
                    leadOutClusters == null) {
                throw new IllegalStateException("Incomplete layout configuration");
            }

            // cohérence structurelle
            if (!(leadInStartClusterInclusive < leadInEndClusterExclusive &&
                    leadInEndClusterExclusive <= utocStartClusterInclusive &&
                    utocStartClusterInclusive <= programStartClusterInclusive &&
                    programStartClusterInclusive < leadOutStartClusterExclusive)) {
                throw new IllegalArgumentException("Incoherent cluster layout");
            }

            int expectedTotalClusters =
                    leadOutStartClusterExclusive + leadOutClusters;

            if (delegate.clusterCount() != expectedTotalClusters) {
                throw new IllegalArgumentException(
                        "Image cluster count mismatch: expected " +
                                expectedTotalClusters + ", got " + delegate.clusterCount()
                );
            }

            return new LayoutCheckedMiniDiscImage(this);
        }
    }

    @Override
    public int clusterCount() {
        return delegate.clusterCount();
    }

    @Override
    public void readSector(int clusterIndex, int sectorIndex, byte[] out2352) throws IOException {
        delegate.readSector(clusterIndex, sectorIndex, out2352);
    }

    @Override
    public void writeSector(int clusterIndex, int sectorIndex, byte[] in2352) throws IOException {
        delegate.writeSector(clusterIndex, sectorIndex, in2352);
    }

    /**
     * Safe API: validates cluster zone + builds a structurally valid audio sector.
     */
    public void writeAudioSector(MiniDiscAddress addr, byte[] audioBlock2332) throws IOException {
        Objects.requireNonNull(addr, "addr");
        validateInProgramArea(addr.clusterIndex());
        // MiniDiscSector validates DATA role + sizes
        MiniDiscSector sector = MiniDiscSector.fromAddressAndAudioBlock(addr, audioBlock2332);
        delegate.writeSector(addr.clusterIndex(), addr.sectorIndex(), sector.rawUnsafe());
    }

    /**
     * Safe API: validates cluster zone + builds a structurally valid TOC/UTOC-like sector.
     */
    public void writeTocSector(MiniDiscAddress addr, byte[] tocData2336) throws IOException {
        Objects.requireNonNull(addr, "addr");
        validateInLeadInOrUtoc(addr.clusterIndex());
        MiniDiscSector sector = MiniDiscSector.fromAddressAndTocData(addr, tocData2336);
        delegate.writeSector(addr.clusterIndex(), addr.sectorIndex(), sector.rawUnsafe());
    }

    private void validateInProgramArea(int clusterIndex) {
        if (clusterIndex < programStartClusterInclusive || clusterIndex >= leadOutStartClusterExclusive) {
            throw new IllegalArgumentException("Cluster not in program area: " + clusterIndex);
        }
    }

    private void validateInLeadInOrUtoc(int clusterIndex) {
        boolean inLeadIn = clusterIndex >= leadInStartClusterInclusive && clusterIndex < leadInEndClusterExclusive;
        boolean inUtoc = clusterIndex >= utocStartClusterInclusive && clusterIndex < utocEndClusterExclusive;
        if (!inLeadIn && !inUtoc) {
            throw new IllegalArgumentException("Cluster not in lead-in/utoc area: " + clusterIndex);
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}