package minidisc.cli;

import minidisc.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

public final class MiniDiscCli {

    // 1 cluster = 36 sectors ; 1 sector = 2352 bytes
    private static final int SECTORS_PER_CLUSTER = 36;
    private static final int SECTOR_BYTES = 2352;
    private static final long CLUSTER_BYTES = (long) SECTORS_PER_CLUSTER * SECTOR_BYTES; // 84_672

    // Convention: TOC payload is 2336 bytes; Lead-out start ADS is BE16 at offset 18
    private static final int TOC_PAYLOAD_BYTES = 2336;
    private static final int TOC_HEADER_BYTES = 16;
    private static final int TOC_LEAD_OUT_START_ADS_OFF = 18; // offset in TOC payload
    private static final long TOC_SECTOR0_START = 0L;         // cluster 0, sector 0

    public static void main(String[] args) {
        if (args.length == 0) {
            usageAndExit(1);
        }

        switch (args[0]) {
            case "create" -> create(slice(args, 1));
            case "open" -> {
                try {
                    open(slice(args, 1));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> usageAndExit(1);
        }
    }

    /**
     * Syntax:
     * md create --type MD60|MD74|MD80 [--force] [--zero-fill] <image-file>
     */
    static void create(String[] args) {
        MiniDiscDiscType type = null;
        boolean force = false;
        boolean zeroFill = false;
        File out = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--type" -> {
                    if (i + 1 >= args.length) die("Missing value after --type");
                    type = parseType(args[++i]);
                }
                case "--force" -> force = true;
                case "--zero-fill" -> zeroFill = true;
                default -> {
                    if (a.startsWith("-")) die("Unknown option: " + a);
                    if (out != null) die("Unexpected extra argument: " + a);
                    out = new File(a);
                }
            }
        }

        if (type == null) die("Missing required --type MD60|MD74|MD80");
        if (out == null) die("Missing output file path");

        try {
            createImage(out, type, force, zeroFill);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createImage(File out, MiniDiscDiscType type, boolean force, boolean zeroFill) throws IOException {
        int totalClusters = MiniDiscLayout.totalClusters(type);
        long totalBytes = MiniDiscFormat.expectedImageBytes(MiniDiscLayout.totalClusters(type));

        if (out.exists()) {
            if (!force) die("File exists: " + out + " (use --force)");
            Files.delete(out.toPath());
        } else {
            File parent = out.getAbsoluteFile().getParentFile();
            if (parent != null) parent.mkdirs();
        }

        try (RandomAccessFile raf = new RandomAccessFile(out, "rw")) {
            raf.setLength(totalBytes);

            if (zeroFill) {
                byte[] zeros = new byte[1024 * 1024];
                raf.seek(0L);
                long remaining = totalBytes;
                while (remaining > 0) {
                    int n = (int) Math.min(zeros.length, remaining);
                    raf.write(zeros, 0, n);
                    remaining -= n;
                }
            }

            // Physically mark disc type in TOC:
            // write leadOutStartAds (programEndExclusive) as BE16 at TOC payload offset 18.
            writeLeadOutStartAdsToTocInPlace(raf, MiniDiscLayout.programEndExclusive(type));

            raf.getFD().sync();
        }

        System.out.println("Created MiniDisc image:");
        System.out.println("  type          : " + type);
        System.out.println("  programEndExcl : 0x" + Integer.toHexString(MiniDiscLayout.programEndExclusive(type)));
        System.out.println("  totalClusters : " + totalClusters);
        System.out.println("  totalBytes    : " + totalBytes);
        System.out.println("  path          : " + out.getAbsolutePath());
    }

    /**
     * Writes 2 bytes in big-endian at TOC payload offset 18 inside the first TOC sector.
     * <p>
     * Convention used here:
     * - TOC is stored in lead-in cluster 0, sector 0.
     * - A TOC sector has 16 bytes header+subheader, then 2336 bytes payload.
     * - The payload field "Lead out start ADS" lives at offset 18 (relative to payload start).
     */
    private static void writeLeadOutStartAdsToTocInPlace(RandomAccessFile raf, int leadOutStartAds) throws IOException {
        if ((leadOutStartAds & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("leadOutStartAds out of 16-bit range: " + leadOutStartAds);
        }

        long tocSectorStart = 0L;                  // cluster 0, sector 0
        long tocPayloadStart = tocSectorStart + 16; // TOC payload starts after 16 bytes
        long pos = tocPayloadStart + TOC_LEAD_OUT_START_ADS_OFF;

        raf.seek(pos);
        raf.writeByte((leadOutStartAds >>> 8) & 0xFF);
        raf.writeByte(leadOutStartAds & 0xFF);
    }

    private static MiniDiscDiscType parseType(String s) {
        try {
            return MiniDiscDiscType.valueOf(s);
        } catch (IllegalArgumentException e) {
            die("Invalid --type: " + s + " (expected MD60|MD74|MD80)");
            return null; // unreachable
        }
    }

    /**
     * Syntax:
     * md open <image-file>
     * <p>
     * Ouvre le fichier en RW, lit le TOC (lead-out start ADS à offset 18), déduit le type,
     * vérifie la taille, et retourne une image "layout-checked".
     */
    static void open(String[] args) throws IOException {
        if (args.length != 1) die("Usage: md open <image-file>");
        File file = new File(args[0]);

        try (OpenedImage opened = openImageRw(file)) {
            System.out.println("Opened MiniDisc image:");
            System.out.println("  path           : " + file.getAbsolutePath());
            System.out.println("  type           : " + opened.type);
            System.out.println("  programEndExcl  : 0x" + Integer.toHexString(MiniDiscLayout.programEndExclusive(opened.type)));
            System.out.println("  totalClusters   : " + MiniDiscLayout.totalClusters(opened.type));
        }
    }

    /**
     * Ouvre l’image en lecture/écriture et retourne:
     * - type disque déduit du TOC
     * - raw image
     * - wrapper LayoutChecked
     */
    static OpenedImage openImageRw(File file) throws IOException {
        if (!file.exists()) throw new IOException("File not found: " + file);

        // 1) Lire leadOutStartAds dans le TOC via RandomAccessFile (RW, mais on ne modifie pas ici)
        int leadOutStartAds;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long pos = TOC_SECTOR0_START + TOC_HEADER_BYTES + TOC_LEAD_OUT_START_ADS_OFF;
            raf.seek(pos);
            leadOutStartAds = readU16BE(raf);
        }

        // 2) Mapper ADS -> type
        MiniDiscDiscType type = typeFromLeadOutStartAds(leadOutStartAds);
        if (type == null) {
            throw new IOException("Unknown leadOutStartAds in TOC: 0x" + Integer.toHexString(leadOutStartAds));
        }

        // 3) Ouvrir l’image brute (RW)
        // NOTE: adaptez selon votre signature réelle.
        MiniDiscImage raw = new FileMiniDiscImage(
                new RandomAccessFile(file, "rw"),
                MiniDiscLayout.totalClusters(type)
        );
        // 4) Vérifier cohérence taille (fortement recommandé)
        long expectedBytes = (long) MiniDiscLayout.totalClusters(type) * CLUSTER_BYTES;
        long actualBytes = file.length();
        if (actualBytes != expectedBytes) {
            raw.close();
            throw new IOException("Image size mismatch: expected " + expectedBytes + " bytes, got " + actualBytes);
        }

        // 5) Wrapper layout-checked
        LayoutCheckedMiniDiscImage checked = switch (type) {
            case MD60 -> MiniDiscImages.md60(raw);
            case MD74 -> MiniDiscImages.md74(raw);
            case MD80 -> MiniDiscImages.md80(raw);
        };

        return new OpenedImage(type, raw, checked);
    }

    private static MiniDiscDiscType typeFromLeadOutStartAds(int leadOutStartAds) {
        for (MiniDiscDiscType t : MiniDiscDiscType.values()) {
            if (MiniDiscLayout.programEndExclusive(t) == leadOutStartAds) return t;
        }
        return null;
    }

    private static int readU16BE(RandomAccessFile raf) throws IOException {
        int hi = raf.readUnsignedByte();
        int lo = raf.readUnsignedByte();
        return (hi << 8) | lo;
    }

    private static void usageAndExit(int code) {
        System.err.println("""
                Usage:
                  md create --type MD60|MD74|MD80 [--force] [--zero-fill] <image-file>
                """);
        System.exit(code);
    }

    private static void die(String msg) {
        System.err.println("Error: " + msg);
        usageAndExit(2);
    }

    private static String[] slice(String[] a, int from) {
        if (from >= a.length) return new String[0];
        String[] out = new String[a.length - from];
        System.arraycopy(a, from, out, 0, out.length);
        return out;
    }

    static final class OpenedImage implements AutoCloseable {
        final MiniDiscDiscType type;
        final MiniDiscImage raw;
        final LayoutCheckedMiniDiscImage checked;

        OpenedImage(MiniDiscDiscType type, MiniDiscImage raw, LayoutCheckedMiniDiscImage checked) {
            this.type = type;
            this.raw = raw;
            this.checked = checked;
        }

        @Override
        public void close() throws IOException {
            checked.close(); // ferme le delegate
        }
    }
}
