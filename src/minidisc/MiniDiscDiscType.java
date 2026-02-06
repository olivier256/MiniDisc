package minidisc;

    /**
    1 sound group = 11.6ms
    11 sound groups = 2 secteurs audio
    1 cluster
    = 32 secteurs audio
    = 16 x 2 secteurs audio
    = 16 x 11 sound groups
    = 176 sound groups
    = 2,0416ms
     @see <A href="https://www.minidisc.org/French_tech/section4.html">Structure des données sur le disque</A>
     */
public enum MiniDiscDiscType {
    MD60(1764),
    /*
    @see <a href="https://www.minidisc.org/French_tech/section11.html">Les types de MiniDisc et leurs structures</a>
    $8CC - $32 = $89A = 2202 clusters
    */
    MD74(2202),
    MD80(2352);

    private final int programClusters;

    MiniDiscDiscType(int programClusters) {
        this.programClusters = programClusters;
    }

    public int programClusters() {
        return programClusters;
    }

    public int programEndExclusive() {
        return MiniDiscLayout.PROGRAM_START + programClusters;
    }

    public int totalClusters() {
        return programEndExclusive() + MiniDiscLayout.LEAD_OUT_CLUSTERS;
    }
}