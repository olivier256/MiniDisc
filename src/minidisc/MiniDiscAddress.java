package minidisc;

public record MiniDiscAddress(int cluster, int sector) {
    public MiniDiscAddress {
        if (sector < 0 || sector >= MiniDiscFormat.SECTORS_PER_CLUSTER) throw new IllegalArgumentException("" + sector);
    }
}