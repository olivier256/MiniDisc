package minidisc;

import java.io.IOException;

/**
 * C’est le point critique : conversion sound groups 424 bytes ↔ payload sector data bytes (2332), avec le pattern 5,5 soundgroups/secteur.
 * Implémentation basée sur MdImage + UTOC start/end ADS :
 * MdSoundGroupWriter :
 * •	reçoit des sg424
 * •	packe dans les secteurs data successifs
 * •	gère les moitiés de sound group à cheval sur 2 secteurs
 * MdSoundGroupReader :
 * •	inverse l’opération
 */
public interface SoundGroupStream {
    void writeSoundGroup(byte[] sg424) throws IOException;

    int readSoundGroup(byte[] sg424) throws IOException; // returns -1 EOF
}