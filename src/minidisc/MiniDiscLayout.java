package minidisc;

public final class MiniDiscLayout {
    public static final int LEAD_IN_START = 0x0000;
    public static final int LEAD_IN_END_EXCL = 0x0003;

    public static final int UTOC_START = 0x0003;
    public static final int UTOC_END_EXCL = 0x0032;
    public static final int PROGRAM_START = 0x0032;

    public static final int LEAD_OUT_CLUSTERS = 115;

    private MiniDiscLayout() {
    }

    public static int leadOutStartExcl(MiniDiscDiscType type) {
        return PROGRAM_START + type.programClusters();
    }

    public static int totalClusters(MiniDiscDiscType type) {
        return leadOutStartExcl(type) + LEAD_OUT_CLUSTERS;
    }
}