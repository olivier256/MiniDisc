package minidisc;

public enum SectorRole {
    DATA,
    LINK,
    SUBDATA;

    /**
     *
     * @param sectorIndex la position du secteur dans le cluster, côté logiciel
     *                    <UL>
     *                    <LI>0…31 --> DATA
     *                    <LI>32…34 --> LINK
     *                    <LI>35 --> SUBDATA
     *                    </UL>
     */
    public static SectorRole fromSectorIndex(int sectorIndex) {
        if (sectorIndex < 0 || sectorIndex >= 36) {
            throw new IllegalArgumentException("sectorIndex out of range: " + sectorIndex);
        }
        if (sectorIndex < 32) {
            return DATA;
        }
        if (sectorIndex < 35) {
            return LINK;
        }
        return SUBDATA;
    }

    /**
     *
     * @param sectorIndex la position du secteur dans le cluster, côté logiciel
     *                    <UL>
     *                    <LI>0…31 --> DATA
     *                    <LI>32…34 --> LINK
     *                    <LI>35 --> SUBDATA
     *                    </UL>
     */
    static SectorRole fromSectorIndexUnsafe(int sectorIndex) {
        if (sectorIndex < 32) return DATA;
        if (sectorIndex < 35) return LINK;
        return SUBDATA;
    }

    /**
     * Returns the MiniDisc sector address byte corresponding to this role
     * and the logical sectorIndex.
     * <ul>
     * <li>DATA    : 0x00..0x1F
     * <li>LINK    : 0xFC..0xFE
     * <li>SUBDATA : 0xFF
     * </ul>
     */
    public byte toSectorAddress(int sectorIndex) {
        return switch (this) {
            case DATA -> (byte) sectorIndex; // 0x00..0x1F
            case LINK -> (byte) (0xFC + (sectorIndex - 32)); // 32->FC, 33->FD, 34->FE
            case SUBDATA -> (byte) 0xFF;
        };
    }
}