package minidisc;

import java.io.IOException;

/**
 * Le texte que vous avez cité insiste sur : “UTOC allocation like floppy”. Donc créez un allocateur séparé.
 * Au début, vous pouvez faire “contiguous append” (comme un enregistreur simple), puis évoluer vers une vraie free-list.
 */
public interface ClusterAllocator {
    int allocateContiguous(int clusters) throws IOException;

    void freeRange(int startCluster, int count) throws IOException;
}