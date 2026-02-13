package wav;

import java.io.IOException;

public interface PcmFrameSource extends AutoCloseable {
    /**
     * @return number of frames read, or -1 on EOF
     */
    int readFrames(short[] dst) throws IOException;
}