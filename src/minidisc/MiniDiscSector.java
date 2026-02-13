package minidisc;

import java.util.Arrays;
import java.util.Objects;

public final class MiniDiscSector {
    // Layout common
    public static final int SYNC_BYTES = 12;
    public static final int HEADER_OFFSET = 12; // after sync
    public static final int MODE_OFFSET = 15;   // HEADER_OFFSET + 3
    public static final int TOC_DATA_OFFSET = 16;   // no 4x00 separator
    public static final int AUDIO_SEP_OFFSET = 16;  // 4x00 separator
    public static final int AUDIO_BLOCK_OFFSET = 20;

    public static final int TOC_DATA_BYTES = MiniDiscFormat.SECTOR_BYTES - TOC_DATA_OFFSET;     // 2336
    public static final int AUDIO_BLOCK_BYTES = MiniDiscFormat.SECTOR_BYTES - AUDIO_BLOCK_OFFSET; // 2332

    /* CD / CD-ROM Mode 2   */
    private static final byte bFF = (byte) 0xFF;
    private static final byte[] DEFAULT_SYNC = { 0x00, bFF , bFF , bFF , bFF , bFF , bFF , bFF , bFF , bFF , bFF, 00};

    private final byte[] raw2352; // always 2352

    private MiniDiscSector(byte[] raw2352) {
        if (raw2352 == null || raw2352.length != MiniDiscFormat.SECTOR_BYTES) {
            throw new IllegalArgumentException("raw must be exactly " + MiniDiscFormat.SECTOR_BYTES + " bytes");
        }
        this.raw2352 = raw2352;
    }

    /**
     * Safe default: copies the input array.
     */
    public static MiniDiscSector fromRaw(byte[] raw2352) {
        Objects.requireNonNull(raw2352, "raw2352");
        if (raw2352.length != MiniDiscFormat.SECTOR_BYTES) {
            throw new IllegalArgumentException("raw must be exactly " + MiniDiscFormat.SECTOR_BYTES + " bytes");
        }
        return new MiniDiscSector(Arrays.copyOf(raw2352, MiniDiscFormat.SECTOR_BYTES));
    }

    /**
     * Optional perf variant: wraps without copying. Use carefully.
     */
    public static MiniDiscSector wrapRaw(byte[] raw2352) {
        return new MiniDiscSector(raw2352);
    }

    public static MiniDiscSector fromAddressWithLinkAndZeroPayload(MiniDiscAddress addr) {
        Objects.requireNonNull(addr, "addr");
        if (addr.sectorRole() != SectorRole.LINK) {
            throw new IllegalArgumentException("LINK payload can only be written to LINK sectors (32..34)");
        }
        return fromAddressAndData2332Layout(addr, new byte[AUDIO_BLOCK_BYTES]);
    }

    public static MiniDiscSector fromAddressWithSubdataAndZeroPayload(MiniDiscAddress addr) {
        Objects.requireNonNull(addr, "addr");
        if (addr.sectorRole() != SectorRole.SUBDATA) {
            throw new IllegalArgumentException("SUBDATA payload can only be written to SUBDATA sector (35)");
        }
        return fromAddressAndData2332Layout(addr, new byte[AUDIO_BLOCK_BYTES]);
    }

    /**
     * Same physical layout as your audio sectors (16B header + 4x00 + 2332 data),
     * but semantically not "audio".
     */
    private static MiniDiscSector fromAddressAndData2332Layout(MiniDiscAddress addr, byte[] data2332) {
        if (data2332.length != AUDIO_BLOCK_BYTES) {
            throw new IllegalArgumentException("Data must be exactly " + AUDIO_BLOCK_BYTES + " bytes");
        }

        byte[] raw = new byte[MiniDiscFormat.SECTOR_BYTES];

        System.arraycopy(DEFAULT_SYNC, 0, raw, 0, SYNC_BYTES);

        addr.writeAddressToHeader(raw, HEADER_OFFSET);
        raw[MODE_OFFSET] = MiniDiscSectorMode.MINI_DISC.code();

        // separator (zeros)
        raw[AUDIO_SEP_OFFSET] = 0;
        raw[AUDIO_SEP_OFFSET + 1] = 0;
        raw[AUDIO_SEP_OFFSET + 2] = 0;
        raw[AUDIO_SEP_OFFSET + 3] = 0;

        System.arraycopy(data2332, 0, raw, AUDIO_BLOCK_OFFSET, AUDIO_BLOCK_BYTES);

        return new MiniDiscSector(raw);
    }


    /**
     * Audio sector: header + 4x00 separator + 2332-byte Audio Block. Mode forced to 2.
     */
    public static MiniDiscSector fromAddressAndAudioBlock(MiniDiscAddress addr, byte[] audioBlock2332) {
        Objects.requireNonNull(addr, "addr");
        Objects.requireNonNull(audioBlock2332, "audioBlock2332");

        if (addr.sectorRole() != SectorRole.DATA) {
            throw new IllegalArgumentException("AudioBlock can only be written to DATA sectors (0..31)");
        }
        if (audioBlock2332.length != AUDIO_BLOCK_BYTES) {
            throw new IllegalArgumentException("AudioBlock must be exactly " + AUDIO_BLOCK_BYTES + " bytes");
        }
        return fromAddressAndData2332Layout(addr, audioBlock2332);
    }

    /**
     * TOC/UTOC-like sector: header immediately followed by 2336 bytes data. Mode forced to 2.
     */
    public static MiniDiscSector fromAddressAndTocData(MiniDiscAddress addr, byte[] tocData2336) {
        Objects.requireNonNull(addr, "addr");
        Objects.requireNonNull(tocData2336, "tocData2336");

        if (tocData2336.length != TOC_DATA_BYTES) {
            throw new IllegalArgumentException("TOC data must be exactly " + TOC_DATA_BYTES + " bytes");
        }

        byte[] raw = new byte[MiniDiscFormat.SECTOR_BYTES];

        System.arraycopy(DEFAULT_SYNC, 0, raw, 0, SYNC_BYTES);

        addr.writeAddressToHeader(raw, HEADER_OFFSET);
        raw[MODE_OFFSET] = MiniDiscSectorMode.MINI_DISC.code();

        // TOC data starts right after header at offset 16
        System.arraycopy(tocData2336, 0, raw, TOC_DATA_OFFSET, TOC_DATA_BYTES);

        return new MiniDiscSector(raw);
    }
    /**
     * Safe copy out.
     */
    public byte[] toRaw2352() {
        return Arrays.copyOf(raw2352, MiniDiscFormat.SECTOR_BYTES);
    }

    /**
     * Internal/fast path. Do not mutate unless you own the instance.
     */
    byte[] rawUnsafe() {
        return raw2352;
    }
}