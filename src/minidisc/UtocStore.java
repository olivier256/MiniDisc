package minidisc;

import java.io.IOException;

public interface UtocStore {
    Utoc read(MiniDiscImage image) throws IOException;

    void write(MiniDiscImage image, Utoc utoc) throws IOException; // commit
}