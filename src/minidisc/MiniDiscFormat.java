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

    // Program area (audio)
    /*
    @see https://www.minidisc.org/French_tech/section11.html
    $8CC - $32 = $89A = 2202 clusters
    */
    public static final int MD74_AUDIO_CLUSTERS = 2202;

    public static final int MD74_LEAD_OUT_START =
            LEAD_IN_CLUSTERS + UTOC_AND_SYSTEM_CLUSTERS + MD74_AUDIO_CLUSTERS;

    public static final int MD74_TOTAL_CLUSTERS =
            LEAD_IN_CLUSTERS + UTOC_AND_SYSTEM_CLUSTERS + MD74_AUDIO_CLUSTERS + LEAD_OUT_CLUSTERS;
    /*
    @see https://www.minidisc.org/French_tech/section4.html
    1 cluster = 176 soundgroups x 11,6 ms = 2,0416 secondes
    60 minutes = 3600s ; 3600 / 2.0416 ~ 1763.6 ~ 1763 (=> 50 min 59.34s)
     */
    public static final int MD60_AUDIO_CLUSTERS = 1763;

    public static final int MD_60_LEAD_OUT_START =
            LEAD_IN_CLUSTERS + UTOC_AND_SYSTEM_CLUSTERS + MD60_AUDIO_CLUSTERS;

    public static final int MD60_TOTAL_CLUSTERS =
            LEAD_IN_CLUSTERS + UTOC_AND_SYSTEM_CLUSTERS + MD60_AUDIO_CLUSTERS + LEAD_OUT_CLUSTERS;


    public static long expectedImageBytes(int totalClusters) {
        return (long) totalClusters * SECTORS_PER_CLUSTER * SECTOR_BYTES;
    }

    private MiniDiscFormat() {
    }
}