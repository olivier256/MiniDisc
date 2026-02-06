package minidisc;

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
    private final int nbOfClusters;

    public FileMiniDiscImage(RandomAccessFile raf, int nbOfClusters) throws IOException {
        this.raf = Objects.requireNonNull(raf, "raf");
        if (nbOfClusters <= 0) throw new IllegalArgumentException("nbOfClusters must be > 0: " + nbOfClusters);
        this.nbOfClusters = nbOfClusters;

        long expectedSize = expectedSizeBytes(nbOfClusters);
        long actualSize = raf.length();
        if (actualSize != expectedSize) {
            throw new IllegalArgumentException("Invalid image size. expected=" + expectedSize + " actual=" + actualSize);
        }
    }

    public static long expectedSizeBytes(int nbOfClusters) {
        return (long) nbOfClusters * MiniDiscFormat.SECTORS_PER_CLUSTER * MiniDiscFormat.SECTOR_BYTES;
    }

    @Override
    public int nbOfClusters() {
        return nbOfClusters;
    }

    @Override
    public void readSector(int clusterIndex, int sectorIndex, byte[] out2352) throws IOException {
        validateAddress(clusterIndex, sectorIndex);
        Objects.requireNonNull(out2352, "out2352");
        if (out2352.length != MiniDiscFormat.SECTOR_BYTES) {
            throw new IllegalArgumentException("out2352 must be exactly " + MiniDiscFormat.SECTOR_BYTES + " bytes");
        }

        raf.seek(byteOffset(clusterIndex, sectorIndex));
        raf.readFully(out2352);
    }

    @Override
    public void writeSector(int clusterIndex, int sectorIndex, byte[] in2352) throws IOException {
        validateAddress(clusterIndex, sectorIndex);
        Objects.requireNonNull(in2352, "in2352");
        if (in2352.length != MiniDiscFormat.SECTOR_BYTES) {
            throw new IllegalArgumentException("in2352 must be exactly " + MiniDiscFormat.SECTOR_BYTES + " bytes");
        }

        raf.seek(byteOffset(clusterIndex, sectorIndex));
        raf.write(in2352);
    }

    private void validateAddress(int clusterIndex, int sectorIndex) {
        if (clusterIndex < 0 || clusterIndex >= nbOfClusters) {
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

    @Override
    public void close() throws IOException {
        raf.close();
    }
}