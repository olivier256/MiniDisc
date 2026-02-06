package minidisc;

import java.io.Closeable;
import java.io.IOException;

public interface MiniDiscImage extends Closeable {
    int nbOfClusters();

    void readSector(int clusterIndex, int sectorIndex, byte[] out2352) throws IOException;

    void writeSector(int clusterIndex, int sectorIndex, byte[] in2352) throws IOException;

}