package minidisc;

public final class MiniDiscAddress {
    private final int clusterIndex; // 0..N
    private final int sectorIndex;  // 0..35

    public MiniDiscAddress(int clusterIndex, int sectorIndex) {
        if (clusterIndex < 0 || clusterIndex > 0xFFFF) {
            throw new IllegalArgumentException("clusterIndex out of range: " + clusterIndex);
        }
        if (sectorIndex < 0 || sectorIndex >= 36) {
            throw new IllegalArgumentException("sectorIndex out of range: " + sectorIndex);
        }
        this.clusterIndex = clusterIndex;
        this.sectorIndex = sectorIndex;
    }

    public int clusterIndex() {
        return clusterIndex;
    }

    /**
     * Returns the logical MiniDisc sector address byte (0…35).
     */
    public int sectorIndex() {
        return sectorIndex;
    }

    public SectorRole sectorRole() {
        return SectorRole.fromSectorIndex(sectorIndex);
    }

    /**
     * Returns the MiniDisc sector address byte (0x00…0xFF, as stored in header / subdata).
     */
    public byte sectorAddressByte() {
        return sectorRole().toSectorAddress(sectorIndex);
    }

    /**
     * Writes the address bytes as stored in a MiniDisc sector header:
     * - 2 bytes cluster address (big-endian)
     * - 1 byte sector address
     */
    public void writeToHeader(byte[] raw2352, int offset) {
        raw2352[offset] = (byte) ((clusterIndex >>> 8) & 0xFF);
        raw2352[offset + 1] = (byte) (clusterIndex & 0xFF);
        raw2352[offset + 2] = sectorAddressByte();
    }
}
