package minidisc;

import java.io.Closeable;
import java.io.IOException;

public interface MiniDiscImage extends Closeable {
    int clusterCount();

    void readSector(int clusterIndex, int sectorIndex, byte[] out2352) throws IOException;

    void writeSector(int clusterIndex, int sectorIndex, byte[] in2352) throws IOException;

    // Helpers pratiques
    default void readDataBytes(int clusterIndex, int sectorIndex, byte[] out2332) throws IOException {
    }

    default void writeDataBytes(int clusterIndex, int sectorIndex, byte[] in2332) throws IOException {
    }
}