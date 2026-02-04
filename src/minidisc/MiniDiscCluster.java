package minidisc;

public final class MiniDiscCluster {
    private final byte[][] sectors2352 = new byte[MiniDiscFormat.SECTORS_PER_CLUSTER][MiniDiscFormat.SECTOR_BYTES];

    public byte[] sector(int i) {
        return sectors2352[i];
    }
}

