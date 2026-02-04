package minidisc;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

/**
 * # MD60
 * dd if=/dev/zero of=md60.bin bs=2352 count=$((1928*36))
 * <p>
 * # MD74
 * dd if=/dev/zero of=md74.bin bs=2352 count=$((2367*36))
 */
public final class FileMiniDiscImage implements MiniDiscImage {
    private final RandomAccessFile raf;
    private final int clusters;

    public FileMiniDiscImage(RandomAccessFile raf, int clusters) throws IOException {
        this.raf = Objects.requireNonNull(raf, "raf");
        this.clusters = clusters;

        long expected = MiniDiscFormat.expectedImageBytes(clusters);
        long actual = raf.length();
        if (actual != expected) {
            throw new IllegalStateException(
                    "Invalid MiniDisc image size: expected " + expected + " bytes (" + clusters + " clusters), got " + actual);
        }
    }

    @Override
    public int clusterCount() {
        return clusters;
    }

    @Override
    public void readSector(int clusterIndex, int sectorIndex, byte[] out2352) throws IOException {
        requireAddr(clusterIndex, sectorIndex);
        requireBuf(out2352, MiniDiscFormat.SECTOR_BYTES);

        long pos = byteOffset(clusterIndex, sectorIndex);
        raf.seek(pos);

        try {
            raf.readFully(out2352, 0, MiniDiscFormat.SECTOR_BYTES);
        } catch (EOFException e) {
            // ne devrait jamais arriver si la taille a été validée
            throw new IOException("Unexpected EOF at offset " + pos, e);
        }
    }

    @Override
    public void writeSector(int clusterIndex, int sectorIndex, byte[] in2352) throws IOException {
        requireAddr(clusterIndex, sectorIndex);
        requireBuf(in2352, MiniDiscFormat.SECTOR_BYTES);

        long pos = byteOffset(clusterIndex, sectorIndex);
        raf.seek(pos);
        raf.write(in2352, 0, MiniDiscFormat.SECTOR_BYTES);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    private static void requireBuf(byte[] buf, int expected) {
        if (buf == null || buf.length < expected) {
            throw new IllegalArgumentException("Buffer must be at least " + expected + " bytes");
        }
    }

    private void requireAddr(int clusterIndex, int sectorIndex) {
        if (clusterIndex < 0 || clusterIndex >= clusters) {
            throw new IllegalArgumentException("clusterIndex out of range: " + clusterIndex);
        }
        if (sectorIndex < 0 || sectorIndex >= MiniDiscFormat.SECTORS_PER_CLUSTER) {
            throw new IllegalArgumentException("sectorIndex out of range: " + sectorIndex);
        }
    }

    private static long byteOffset(int clusterIndex, int sectorIndex) {
        long sectorNumber = (long) clusterIndex * MiniDiscFormat.SECTORS_PER_CLUSTER + sectorIndex;
        return sectorNumber * MiniDiscFormat.SECTOR_BYTES;
    }
}
