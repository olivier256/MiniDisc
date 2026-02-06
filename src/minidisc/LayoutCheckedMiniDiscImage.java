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
    private final int programEndClusterExclusive;  // 0x0715 (MD60) or 0x08CC (MD74)
    private final int leadOutClusters;               // typically 115

    public LayoutCheckedMiniDiscImage(
            MiniDiscImage delegate,
            int leadInStartClusterInclusive,
            int leadInEndClusterExclusive,
            int utocStartClusterInclusive,
            int utocEndClusterExclusive,
            int programStartClusterInclusive,
            int programEndClusterExclusive,
            int leadOutClusters
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");

        if (!(leadInStartClusterInclusive < leadInEndClusterExclusive &&
                leadInEndClusterExclusive <= utocStartClusterInclusive &&
                utocEndClusterExclusive <= programStartClusterInclusive &&
                programStartClusterInclusive < programEndClusterExclusive)) {
            throw new IllegalArgumentException("Incoherent cluster layout");
        }
        // Optionnel mais probablement vrai dans votre intention :
        if (utocStartClusterInclusive >= utocEndClusterExclusive) {
            throw new IllegalArgumentException("Invalid UTOC range");
        }

        int expected = programEndClusterExclusive + leadOutClusters;
        if (delegate.nbOfClusters() != expected) {
            throw new IllegalArgumentException(
                    "Image cluster count mismatch: expected " + expected + ", got " + delegate.nbOfClusters()
            );
        }

        this.leadInStartClusterInclusive = leadInStartClusterInclusive;
        this.leadInEndClusterExclusive = leadInEndClusterExclusive;
        this.utocStartClusterInclusive = utocStartClusterInclusive;
        this.utocEndClusterExclusive = utocEndClusterExclusive;
        this.programStartClusterInclusive = programStartClusterInclusive;
        this.programEndClusterExclusive = programEndClusterExclusive;
        this.leadOutClusters = leadOutClusters;
    }

    @Override
    public int nbOfClusters() {
        return delegate.nbOfClusters();
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
        if (programStartClusterInclusive <= clusterIndex && clusterIndex < programEndClusterExclusive) {
            return;
        } else {
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