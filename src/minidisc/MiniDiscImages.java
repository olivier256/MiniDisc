package minidisc;

public final class MiniDiscImages {
    private MiniDiscImages() {
    }

    public static LayoutCheckedMiniDiscImage of(
            MiniDiscDiscType type,
            MiniDiscImage raw
    ) {
        return new LayoutCheckedMiniDiscImage(
                raw,
                MiniDiscLayout.LEAD_IN_START,
                MiniDiscLayout.LEAD_IN_END_EXCL,
                MiniDiscLayout.UTOC_START,
                MiniDiscLayout.UTOC_END_EXCL,
                MiniDiscLayout.PROGRAM_START,
                MiniDiscLayout.leadOutStartExcl(type),
                MiniDiscLayout.LEAD_OUT_CLUSTERS
        );
    }

    public static LayoutCheckedMiniDiscImage md60(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD60, raw);
    }

    public static LayoutCheckedMiniDiscImage md74(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD74, raw);
    }
}