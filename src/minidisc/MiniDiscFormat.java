package minidisc;

public final class MiniDiscFormat {
    public static final int SECTOR_BYTES = 2352;
    public static final int SECTOR_DATA_BYTES = 2332;   // payload
    public static final int SECTORS_PER_CLUSTER = 36;
    public static final int DATA_SECTORS_PER_CLUSTER = 32;
    public static final int LINK_SECTORS_PER_CLUSTER = 3;
    public static final int SUBDATA_SECTORS_PER_CLUSTER = 1;

    // Layout (clusters)
    public static final int LEAD_IN_CLUSTERS = 3;
    public static final int UTOC_AND_SYSTEM_CLUSTERS = 47;
    public static final int LEAD_OUT_CLUSTERS = 115; // clusters 08CC–093E sur MD74 ; on garde 115 aussi pour MD60

    public static long expectedImageBytes(int totalClusters) {
        return (long) totalClusters * SECTORS_PER_CLUSTER * SECTOR_BYTES;
    }

    private MiniDiscFormat() {
    }
}