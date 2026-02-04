package minidisc;

import java.util.Arrays;
import java.util.Objects;

import static minidisc.MiniDiscFormat.SECTOR_BYTES;

public final class MiniDiscSector {
    public static final int SYNC_BYTES = 12;
    public static final int HEADER_BYTES = 4;     // 2 cluster + 1 sector + 1 mode
    public static final int SEP_BYTES = 4;        // 4x00
    public static final int HEADER_OFFSET = 12;   // after sync
    public static final int SEP_OFFSET = 16;      // after sync+header
    public static final int AUDIO_BLOCK_OFFSET = 20;  // after sync+header+sep
    public static final int AUDIO_BLOCK_BYTES = SECTOR_BYTES - AUDIO_BLOCK_OFFSET; // 2332

    // TODO: remplacer par la vraie séquence si vous la figez.
    private static final byte[] DEFAULT_SYNC = new byte[SYNC_BYTES];

    private final byte[] raw; // always 2352

    /**
     * Beware: wraps without copying. Use carefully.
     */
    private MiniDiscSector(byte[] raw2352) {
        if (raw2352 == null || raw2352.length != SECTOR_BYTES) {
            throw new IllegalArgumentException("raw must be exactly " + SECTOR_BYTES + " bytes");
        }
        this.raw = raw2352;
    }

    /**
     * Safe default: copies the input array.
     */
    public static MiniDiscSector fromRaw(byte[] raw2352) {
        Objects.requireNonNull(raw2352, "raw2352");
        if (raw2352.length != SECTOR_BYTES) {
            throw new IllegalArgumentException("raw must be exactly " + SECTOR_BYTES + " bytes");
        }
        return new MiniDiscSector(Arrays.copyOf(raw2352, SECTOR_BYTES));
    }

    /**
     * Optional perf variant: wraps without copying. Use carefully.
     */
    public static MiniDiscSector wrapRaw(byte[] raw2352) {
        return new MiniDiscSector(raw2352);
    }

    /**
     * Builds an audio DATA sector (mode=2) from address + full 2332-byte payload.
     */
    public static MiniDiscSector fromAddressAndAudioBlock(MiniDiscAddress addr, byte[] audioBlock2332) {
        Objects.requireNonNull(addr, "addr");
        Objects.requireNonNull(audioBlock2332, "payload2332");

        if (addr.sectorRole() != SectorRole.DATA) {
            throw new IllegalArgumentException("Audio payload can only be written to DATA sectors (0..31)");
        }
        if (audioBlock2332.length != AUDIO_BLOCK_BYTES) {
            throw new IllegalArgumentException("payload must be exactly " + AUDIO_BLOCK_BYTES + " bytes");
        }

        byte[] raw = new byte[SECTOR_BYTES];

        // sync (12)
        System.arraycopy(DEFAULT_SYNC, 0, raw, 0, SYNC_BYTES);

        // header: address (3) + mode (1)
        addr.writeToHeader(raw, HEADER_OFFSET); // writes clusterHi, clusterLo, sectorAddressByte
        raw[HEADER_OFFSET + 3] = 0x02;          // mode = 2 (MiniDisc)

        // separator 4x00 already zero by default, but keep explicitness if you want:
        raw[SEP_OFFSET] = 0;
        raw[SEP_OFFSET + 1] = 0;
        raw[SEP_OFFSET + 2] = 0;
        raw[SEP_OFFSET + 3] = 0;

        // payload (2332)
        System.arraycopy(audioBlock2332, 0, raw, AUDIO_BLOCK_OFFSET, AUDIO_BLOCK_BYTES);

        return new MiniDiscSector(raw);
    }

    /**
     * Returns a copy to preserve immutability.
     */
    public byte[] toRaw() {
        return Arrays.copyOf(raw, SECTOR_BYTES);
    }

    /**
     * Internal use only (if you want zero-copy pipelines, expose carefully).
     */
    public byte[] rawUnsafe() {
        return raw;
    }
}
