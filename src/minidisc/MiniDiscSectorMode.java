package minidisc;

public enum MiniDiscSectorMode {
    ZERO_MODE(0),
    RESERVED(1),
    MINI_DISC(2),
    // 3..255 prohibited

    ;

    private final int code;

    MiniDiscSectorMode(int code) {
        this.code = code;
    }

    public byte code() {
        return (byte) (code & 0xFF);
    }

    public static MiniDiscSectorMode fromCode(int code) {
        return switch (code) {
            case 0 -> ZERO_MODE;
            case 1 -> RESERVED;
            case 2 -> MINI_DISC;
            default -> throw new IllegalArgumentException("Unexpected value: " + code);
        };
    }
}