package minidisc;

public enum MiniDiscDiscType {
    /*
    @see https://www.minidisc.org/French_tech/section4.html
    1 cluster = 176 soundgroups x 11,6 ms = 2,0416 secondes
    60 minutes = 3600s ; 3600 / 2.0416 ~ 1763.6 ~ 1763 (=> 50 min 59.34s)
     */
    MD60(1763),
    /*
    @see https://www.minidisc.org/French_tech/section11.html
    $8CC - $32 = $89A = 2202 clusters
    */
    MD74(2202);

    private final int programClusters;

    MiniDiscDiscType(int programClusters) {
        this.programClusters = programClusters;
    }

    public int programClusters() {
        return programClusters;
    }

    public int leadOutStartExclusive() {
        return MiniDiscLayout.PROGRAM_START + programClusters;
    }

    public int totalClusters() {
        return leadOutStartExclusive() + MiniDiscLayout.LEAD_OUT_CLUSTERS;
    }
}