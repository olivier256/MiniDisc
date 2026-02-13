package wav;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public final class WavPcmReader implements PcmFrameSource {

    private final AudioInputStream pcmStream;
    private int offset;

    public WavPcmReader(File inputWav) {
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(inputWav);
            AudioFormat baseFormat = ais.getFormat();

            // On force en PCM 16-bit little-endian stéréo 44.1 kHz
            AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100f,
                    16,
                    2,
                    4,          // frame size = 2 channels * 2 bytes
                    44100f,
                    false       // little endian
            );

            pcmStream = AudioSystem.getAudioInputStream(target, ais);

        } catch (UnsupportedAudioFileException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

    }


    @Override
    public int readFrames(short[] dst) throws IOException {
        return 0;
    }

    @Override
    public void close() throws Exception {
        pcmStream.close();
    }
}
