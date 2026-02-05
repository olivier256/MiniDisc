package minidisc;

public final class MiniDiscImages {
    private MiniDiscImages() {
    }

    public static LayoutCheckedMiniDiscImage of(
            MiniDiscDiscType type,
            MiniDiscImage raw
    ) {
        return new LayoutCheckedMiniDiscImage.Builder(raw)
                .withLeadInStartClusterInclusive(MiniDiscLayout.LEAD_IN_START)
                .withLeadInEndClusterExclusive(MiniDiscLayout.LEAD_IN_END_EXCL)
                .withUtocStartClusterInclusive(MiniDiscLayout.UTOC_START)
                .withProgramStartClusterInclusive(MiniDiscLayout.PROGRAM_START)
                .withLeadOutStartClusterExclusive(
                        MiniDiscLayout.leadOutStartExcl(type)
                )
                .withLeadOutClusters(MiniDiscLayout.LEAD_OUT_CLUSTERS)
                .build();
    }

    public static LayoutCheckedMiniDiscImage md60(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD60, raw);
    }

    public static LayoutCheckedMiniDiscImage md74(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD74, raw);
    }
}