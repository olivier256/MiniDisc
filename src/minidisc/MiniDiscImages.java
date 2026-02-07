package minidisc;

public final class MiniDiscImages {
    private MiniDiscImages() {
    }

    private static LayoutCheckedMiniDiscImage of(
            MiniDiscDiscType type,
            MiniDiscImage raw
    ) {
        return new LayoutCheckedMiniDiscImage(
                raw,
                type
        );
    }

    public static LayoutCheckedMiniDiscImage md60(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD60, raw);
    }

    public static LayoutCheckedMiniDiscImage md74(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD74, raw);
    }

    public static LayoutCheckedMiniDiscImage md80(MiniDiscImage raw) {
        return of(MiniDiscDiscType.MD80, raw);
    }
}