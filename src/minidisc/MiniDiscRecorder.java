package minidisc;

import java.io.IOException;
import java.util.Objects;

import static minidisc.MiniDiscFormat.DATA_SECTORS_PER_CLUSTER;

/**
 * High-level recording API: PCM stream -> ATRAC1 SP clusters -> sectors on disc image.
 * <p>
 * Conventions (your current model):
 * - 1 cluster = 36 sectors (2352 bytes each)
 * - sectors 0..31 : audio data sectors
 * - sectors 32..34: link sectors
 * - sector 35     : subdata sector
 * - For now, link/subdata sectors: header only + zero payload
 */
public final class MiniDiscRecorder implements AutoCloseable {

    public static final int FRAMES_PER_SOUNDGROUP = 512;
    public static final int SOUNDGROUPS_PER_CLUSTER = 176;
    public static final int FRAMES_PER_CLUSTER = FRAMES_PER_SOUNDGROUP * SOUNDGROUPS_PER_CLUSTER; // 90112

    private final LayoutCheckedMiniDiscImage image;
    private final Atrac1SpClusterEncoder encoder;

    private final short[] pcmInterleavedCluster; // [L0,R0,L1,R1,...]
    private int bufferedFrames;                 // 0..FRAMES_PER_CLUSTER

    private int nextProgramCluster;             // absolute cluster index in disc image
    private boolean closed;

    public MiniDiscRecorder(LayoutCheckedMiniDiscImage image,
                            Atrac1SpClusterEncoder encoder,
                            int startProgramClusterInclusive) {
        this.image = Objects.requireNonNull(image, "image");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.pcmInterleavedCluster = new short[FRAMES_PER_CLUSTER * 2];
        this.bufferedFrames = 0;
        this.nextProgramCluster = startProgramClusterInclusive;
    }

    /**
     * Appends PCM frames (stereo interleaved, 16-bit).
     *
     * @param interleavedStereo [L,R,L,R,...]
     * @param offsetFrames      offset in frames (not samples)
     * @param frameCount        number of stereo frames
     */
    public void writePcmFrames(short[] interleavedStereo, int offsetFrames, int frameCount) throws IOException {
        ensureOpen();
        Objects.requireNonNull(interleavedStereo, "interleavedStereo");
        if (offsetFrames < 0 || frameCount < 0) throw new IllegalArgumentException("negative offset/count");

        int startSample = offsetFrames * 2;
        int samplesToCopy = frameCount * 2;
        if (startSample + samplesToCopy > interleavedStereo.length) {
            throw new IllegalArgumentException("Buffer too small for offset/count");
        }

        int framesRemaining = frameCount;
        int srcFramePos = offsetFrames;

        while (framesRemaining > 0) {
            int room = FRAMES_PER_CLUSTER - bufferedFrames;
            int n = Math.min(room, framesRemaining);

            int dstSamplePos = bufferedFrames * 2;
            int srcSamplePos = srcFramePos * 2;
            System.arraycopy(interleavedStereo, srcSamplePos, pcmInterleavedCluster, dstSamplePos, n * 2);

            bufferedFrames += n;
            srcFramePos += n;
            framesRemaining -= n;

            if (bufferedFrames == FRAMES_PER_CLUSTER) {
                writeBufferedCluster(false);
            }
        }
    }

    /**
     * Finalizes recording (like pressing STOP):
     * - pads the last partial cluster with zeros
     * - writes it
     */
    public void finalizeRecording() throws IOException {
        ensureOpen();
        if (bufferedFrames > 0) {
            // zero pad tail
            int fromSample = bufferedFrames * 2;
            for (int i = fromSample; i < pcmInterleavedCluster.length; i++) {
                pcmInterleavedCluster[i] = 0;
            }
            writeBufferedCluster(true);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        try {
            finalizeRecording();
        } finally {
            closed = true;
        }
    }

    private void writeBufferedCluster(boolean lastCluster) throws IOException {
        // 1) Encode PCM cluster -> 32 audio sectors (raw 2352 bytes)
        MiniDiscSector[] audioSectors = encoder.encodeClusterToAudioSectors(pcmInterleavedCluster, lastCluster);
        if (audioSectors.length != DATA_SECTORS_PER_CLUSTER) {
            throw new IllegalStateException("Encoder must return " + DATA_SECTORS_PER_CLUSTER
                    + " audio sectors, got " + audioSectors.length);
        }

        int clusterIndex = nextProgramCluster;

        // 2) Write audio sectors 0..31
        for (int s = 0; s < DATA_SECTORS_PER_CLUSTER; s++) {
            image.writeSector(clusterIndex, s, audioSectors[s].rawUnsafe());
        }

        // 3) Write link sectors 32..34 (header only + zeros)
        for (int s = 32; s <= 34; s++) {
            MiniDiscSector linkSector =
                    MiniDiscSector.fromAddressWithLinkAndZeroPayload(
                            new MiniDiscAddress(clusterIndex, s)
                    );
            image.writeSector(clusterIndex, s, linkSector.rawUnsafe());
        }

        writeSubdataSector(clusterIndex);

        nextProgramCluster++;
        bufferedFrames = 0;
    }

    private void writeSubdataSector(int clusterIndex) throws IOException {
        int s = 35;
        MiniDiscSector subdataSector = MiniDiscSector.fromAddressWithSubdataAndZeroPayload(
                new MiniDiscAddress(clusterIndex, s)
        );
        image.writeSector(clusterIndex, s, subdataSector.rawUnsafe());
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Recorder closed");
    }

    /**
     * Minimal contract: "cluster PCM -> 32 audio sectors".
     * Keep it narrow to avoid leaking codec internals into disk layer.
     */
    public interface Atrac1SpClusterEncoder {
        /**
         * @param pcmInterleavedCluster length must be FRAMES_PER_CLUSTER*2
         * @param lastCluster           true if this cluster contains padding at the end
         */
        MiniDiscSector[] encodeClusterToAudioSectors(short[] pcmInterleavedCluster, boolean lastCluster);
    }
}