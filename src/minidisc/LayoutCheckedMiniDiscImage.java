package minidisc;

import java.io.IOException;
import java.util.Objects;

import static minidisc.MiniDiscFormat.LEAD_OUT_CLUSTERS;

/**
 * Cluster-range validation lives here.
 * Sector structure validation lives in MiniDiscSector factories.
 */
public final class LayoutCheckedMiniDiscImage implements MiniDiscImage {
    private final MiniDiscImage delegate;

    private final int programEndClusterExclusive;  // 0x0715 (MD60) or 0x08CC (MD74)

    public LayoutCheckedMiniDiscImage(
            MiniDiscImage delegate,
            MiniDiscDiscType discType
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");

        int programEndClusterExclusive = MiniDiscLayout.programEndExclusive(discType);
        int expected = programEndClusterExclusive + LEAD_OUT_CLUSTERS;
        if (delegate.nbOfClusters() != expected) {
            throw new IllegalArgumentException(
                    "Image cluster count mismatch: expected " + expected + ", got " + delegate.nbOfClusters()
            );
        }

        this.programEndClusterExclusive = programEndClusterExclusive;
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
        int clusterIndex = addr.clusterIndex();
        validateInLeadInOrUtoc(clusterIndex);
        MiniDiscSector sector = MiniDiscSector.fromAddressAndTocData(addr, tocData2336);
        delegate.writeSector(clusterIndex, addr.sectorIndex(), sector.rawUnsafe());
    }

    private static void validateInLeadInOrUtoc(int clusterIndex) {
        boolean clusterIsInUtoc = clusterIndex >= MiniDiscLayout.UTOC_START && clusterIndex < MiniDiscLayout.UTOC_END_EXCL;
        boolean clusterIsInLeadIn = clusterIndex >= MiniDiscLayout.LEAD_IN_START && clusterIndex < MiniDiscLayout.LEAD_IN_END_EXCL;
        if (clusterIsInLeadIn || clusterIsInUtoc) {
            // legal
        } else {
            throw new IllegalArgumentException("Cluster not in lead-in/utoc area: " + clusterIndex);
        }
    }

    private void validateInProgramArea(int clusterIndex) {
        if (MiniDiscLayout.PROGRAM_START <= clusterIndex && clusterIndex < programEndClusterExclusive) {
            // legal
        } else {
            throw new IllegalArgumentException("Cluster not in program area: " + clusterIndex);
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}