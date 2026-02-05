package minidisc;

import java.io.Closeable;
import java.io.IOException;

public interface MiniDiscImage extends Closeable {
    int clusterCount();

    void readSector(int clusterIndex, int sectorIndex, byte[] out2352) throws IOException;

    void writeSector(int clusterIndex, int sectorIndex, byte[] in2352) throws IOException;

}