package minidisc;

import java.util.Arrays;

public final class MiniDiscSector {
    private final byte[] raw2352;

    public MiniDiscSector(byte[] raw2352) {
        this.raw2352 = raw2352;
    }

    public byte[] data2332View() {
        return Arrays.copyOf(raw2352, raw2352.length);
    }
}