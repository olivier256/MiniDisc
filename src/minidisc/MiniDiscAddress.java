package minidisc;

/**
 * Logical address in the image: cluster + sectorIndex (0..35).
 * This is NOT the “sector address byte” written in headers (FC/FD/... for LINK/SUBDATA).
 */
public record MiniDiscAddress(int clusterIndex, int sectorIndex) {
    public MiniDiscAddress {
        if (clusterIndex < 0 || clusterIndex > 0xFFFF) {
            throw new IllegalArgumentException("clusterIndex out of range: " + clusterIndex);
        }
        if (sectorIndex < 0 || sectorIndex >= MiniDiscFormat.SECTORS_PER_CLUSTER) {
            throw new IllegalArgumentException("sectorIndex out of range: " + sectorIndex);
        }
    }

    public SectorRole sectorRole() {
        return SectorRole.fromSectorIndexUnsafe(sectorIndex);
    }

    public byte sectorAddressByte() {
        return sectorRole().toSectorAddress(sectorIndex);
    }

    /**
     * Writes 2 bytes cluster (big-endian) + 1 byte sector-address into raw sector header.
     */
    public void writeAddressToHeader(byte[] raw2352, int offset) {
        raw2352[offset] = (byte) ((clusterIndex >>> 8) & 0xFF);
        raw2352[offset + 1] = (byte) (clusterIndex & 0xFF);
        raw2352[offset + 2] = sectorAddressByte();
    }
}