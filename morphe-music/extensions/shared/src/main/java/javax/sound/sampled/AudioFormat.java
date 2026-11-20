package javax.sound.sampled;

import java.util.Map;
import java.util.Collections;

/**
 * Minimal port of {@code javax.sound.sampled.AudioFormat} used only to satisfy
 * the signatures of {@code de.sciss.jump3r.lowlevel.LameEncoder}. Android does
 * not ship {@code java.sound}; this stub is compiled into the extension so the
 * encoder can run on device. Only the accessors read by jump3r are provided.
 */
public class AudioFormat {

    public static class Encoding {
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");
        private final String name;

        public Encoding(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Encoding encoding;
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final int frameSize;
    private final float frameRate;
    private final boolean bigEndian;
    private final Map<String, Object> properties;

    public AudioFormat(Encoding encoding, float sampleRate, int sampleSizeInBits,
                       int channels, int frameSize, float frameRate, boolean bigEndian) {
        this.encoding = encoding;
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
        this.bigEndian = bigEndian;
        this.properties = Collections.emptyMap();
    }

    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels,
                       boolean signed, boolean bigEndian) {
        this(signed ? Encoding.PCM_SIGNED : new Encoding("PCM_UNSIGNED"),
                sampleRate, sampleSizeInBits, channels,
                (sampleSizeInBits + 7) / 8 * channels, sampleRate, bigEndian);
    }

    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels,
                       boolean signed, boolean bigEndian, Map<String, Object> properties) {
        this(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public boolean isUnsigned() {
        return encoding != Encoding.PCM_SIGNED;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    @Override
    public String toString() {
        return "AudioFormat[" + encoding + "," + sampleRate + " Hz,"
                + sampleSizeInBits + "-bit," + channels + " ch]";
    }
}