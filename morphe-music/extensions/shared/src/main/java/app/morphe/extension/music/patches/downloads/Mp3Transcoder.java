package app.morphe.extension.music.patches.downloads;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import de.sciss.jump3r.mp3.Lame;
import de.sciss.jump3r.mp3.LameGlobalFlags;
import de.sciss.jump3r.mp3.MPEGMode;
import de.sciss.jump3r.mp3.VbrMode;

/**
 * Transcodes downloaded audio into MP3 (LAME, via the pure-Java jump3r port).
 *
 * <p>The native YT Music audio (AAC in MP4) is decoded to 16-bit PCM with
 * {@link MediaCodec}, then encoded with jump3r. The {@code javax.sound.sampled}
 * types jump3r expects are provided by a small stub compiled into this package.
 */
public final class Mp3Transcoder {

    private Mp3Transcoder() {
    }

    /** @param bitrateKbps target CBR bitrate, e.g. 128 or 320. */
    static int encodePcmToMp3(ShortBuffer pcm, int sampleRate, int channels,
                              int bitrateKbps, OutputStream out) throws IOException {
        Lame lame = new Lame();
        LameGlobalFlags gfp = lame.lame_init();
        gfp.num_channels = channels;
        gfp.in_samplerate = sampleRate;
        gfp.out_samplerate = sampleRate;
        gfp.brate = bitrateKbps;
        gfp.mode = channels == 1 ? MPEGMode.MONO : MPEGMode.JOINT_STEREO;
        gfp.VBR = VbrMode.vbr_off; // CBR keeps the requested quality predictable.
        gfp.quality = 5;
        if (lame.lame_init_params(gfp) < 0) {
            lame.lame_close(gfp);
            throw new IOException("LAME initialization failed");
        }
        byte[] buffer = new byte[16 * 1024];
        int[] left = new int[8192];
        int[] right = new int[8192];
        int total = 0;
        final int limit = pcm.limit();
        final int position = pcm.position();
        final boolean interleaved = channels == 2;
        short[] array = pcm.hasArray() ? pcm.array() : null;
        final int arrayOffset = pcm.hasArray() ? pcm.arrayOffset() : 0;
        // Drain the whole buffer in frames; preserve the original position afterwards.
        while (pcm.position() < limit) {
            int n = Math.min(left.length, limit - pcm.position());
            int at = arrayOffset + pcm.position();
            for (int i = 0; i < n; i++) {
                int v = array != null ? array[at + i] : pcm.get();
                if (interleaved) {
                    if ((i & 1) == 0) left[i >> 1] = v;
                    else right[i >> 1] = v;
                } else {
                    left[i] = v;
                }
            }
            if (!interleaved && n < left.length) {
                for (int i = n; i < left.length; i++) left[i] = 0;
            }
            pcm.position(pcm.position() + n);
            int samplesPerChannel = interleaved ? n >> 1 : n;
            int encoded = lame.lame_encode_buffer_int(gfp, left, right, 0, buffer, 0, samplesPerChannel);
            if (encoded > 0) {
                out.write(buffer, 0, encoded);
                total += encoded;
            }
        }
        int finish = lame.lame_encode_flush(gfp, buffer, 0, buffer.length);
        if (finish > 0) {
            out.write(buffer, 0, finish);
            total += finish;
        }
        lame.lame_close(gfp);
        pcm.position(position);
        return total;
    }

    /**
     * Decodes an audio file (currently AAC in MP4, via MediaCodec) to interleaved
     * 16-bit little-endian PCM and streams the MP3 encoding directly to {@code out}.
     *
     * @return the number of encoded MP3 bytes written.
     */
    static int decodeAndTranscode(File input, int bitrateKbps, OutputStream out) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(input.getAbsolutePath());
            int track = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    break;
                }
            }
            if (track < 0) throw new IOException("No audio track in " + input.getName());
            MediaFormat format = extractor.getTrackFormat(track);
            extractor.selectTrack(track);
            String mime = format.getString(MediaFormat.KEY_MIME);
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            Lame lame = new Lame();
            LameGlobalFlags gfp = lame.lame_init();
            int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
            gfp.num_channels = channels;
            gfp.in_samplerate = sampleRate;
            gfp.out_samplerate = sampleRate;
            gfp.brate = bitrateKbps;
            gfp.mode = channels == 1 ? MPEGMode.MONO : MPEGMode.JOINT_STEREO;
            gfp.VBR = VbrMode.vbr_off;
            gfp.quality = 5;
            if (lame.lame_init_params(gfp) < 0) {
                lame.lame_close(gfp);
                throw new IOException("LAME initialization failed");
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            byte[] mp3Buffer = new byte[16 * 1024];
            int[] left = new int[8192];
            int[] right = new int[8192];
            int total = 0;
            boolean inputDone = false;
            boolean outputDone = false;
            while (!outputDone) {
                if (!inputDone) {
                    int inIndex = codec.dequeueInputBuffer(10_000);
                    if (inIndex >= 0) {
                        ByteBuffer in = codec.getInputBuffer(inIndex);
                        int sampleSize = extractor.readSampleData(in, 0);
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long pts = extractor.getSampleTime();
                            codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0);
                            extractor.advance();
                        }
                    }
                }
                int outIndex = codec.dequeueOutputBuffer(info, 10_000);
                if (outIndex >= 0) {
                    if (info.size > 0) {
                        drainPcm(codec.getOutputBuffer(outIndex), info, channels, left, right, mp3Buffer, lame, gfp, out);
                        total += info.size;
                    }
                    codec.releaseOutputBuffer(outIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                    }
                }
            }
            int finish = lame.lame_encode_flush(gfp, mp3Buffer, 0, mp3Buffer.length);
            if (finish > 0) {
                out.write(mp3Buffer, 0, finish);
                total += finish;
            }
            lame.lame_close(gfp);
            return total;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("MP3 transcoding failed", e);
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                    codec.release();
                } catch (Exception ignored) {
                    // Best effort shutdown.
                }
            }
            extractor.release();
        }
    }

    private static void drainPcm(ByteBuffer pcm, MediaCodec.BufferInfo info, int channels,
                                 int[] left, int[] right, byte[] mp3Buffer,
                                 Lame lame, LameGlobalFlags gfp, OutputStream out) throws IOException {
        pcm.position(info.offset);
        pcm.limit(info.offset + info.size);
        pcm.order(ByteOrder.LITTLE_ENDIAN);
        int samples = info.size / 2;
        int offset = 0;
        while (offset < samples) {
            int n = Math.min(left.length, samples - offset);
            if (channels == 2) {
                for (int i = 0; i < n; i++) {
                    left[i] = pcm.getShort();
                    right[i] = pcm.getShort();
                }
            } else {
                for (int i = 0; i < n; i++) {
                    left[i] = pcm.getShort();
                }
            }
            int encoded = lame.lame_encode_buffer_int(gfp, left, right, 0, mp3Buffer, 0, n);
            if (encoded > 0) {
                out.write(mp3Buffer, 0, encoded);
            }
            offset += n;
        }
    }

    /**
     * MP3 download entry point, mirrors {@link AudioTagger#write}. The source must be an
     * MP4/AAC stream (the common YT Music audio): MediaCodec decodes it to PCM,
     * jump3r encodes MP3, and an ID3v2.3 tag is prepended.
     *
     * @return the number of MP3 bytes written.
     */
    static int write(File input, int bitrateKbps, OutputStream out, AudioTagger.Tags tags) throws IOException {
        byte[] id3 = id3v2(tags);
        out.write(id3, 0, id3.length);
        return id3.length + decodeAndTranscode(input, bitrateKbps, out);
    }

    private static byte[] id3v2(AudioTagger.Tags tags) {
        java.io.ByteArrayOutputStream tag = new java.io.ByteArrayOutputStream();
        textFrame(tag, "TIT2", tags.title, null);
        textFrame(tag, "TPE1", tags.artist, null);
        textFrame(tag, "TALB", tags.album, null);
        if (tags.artwork != null) {
            String mime = AudioTagger.imageMimeType(tags.artwork);
            if (mime != null) pictureFrame(tag, "APIC", mime, tags.artwork);
        }
        byte[] body = tag.toByteArray();
        if (body.length == 0) return new byte[0];
        int size = body.length;
        byte[] header = new byte[10];
        header[0] = 'I';
        header[1] = 'D';
        header[2] = '3';
        header[3] = 3; // version 2.3
        header[4] = 0; // no flags
        // ID3v2.3 uses four 7-bit size bytes.
        header[6] = (byte) ((size >>> 21) & 0x7F);
        header[7] = (byte) ((size >>> 14) & 0x7F);
        header[8] = (byte) ((size >>> 7) & 0x7F);
        header[9] = (byte) (size & 0x7F);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(header, 0, header.length);
        out.write(body, 0, body.length);
        return out.toByteArray();
    }

    private static void textFrame(java.io.ByteArrayOutputStream out, String id, String value, String encoding) {
        if (value == null || value.isEmpty()) return;
        byte[] text;
        if (encoding == null) {
            text = new byte[1 + value.length()];
            text[0] = 0; // ISO-8859-1
            for (int i = 0; i < value.length(); i++) {
                text[1 + i] = (byte) value.charAt(i);
            }
        } else {
            byte[] bytes;
            try {
                bytes = value.getBytes("UTF-16BE");
            } catch (java.io.UnsupportedEncodingException impossible) {
                bytes = new byte[0];
            }
            text = new byte[1 + bytes.length];
            text[0] = 1; // UTF-16 with BOM
            System.arraycopy(bytes, 0, text, 1, bytes.length);
        }
        frame(out, id, text);
    }

    private static void pictureFrame(java.io.ByteArrayOutputStream out, String id, String mime, byte[] picture) {
        byte[] mimeBytes = mime.getBytes();
        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
        body.write(mimeBytes, 0, mimeBytes.length);
        body.write(0);
        body.write(0); // picture type: cover (front)
        byte[] description = new byte[0];
        body.write(description, 0, description.length);
        body.write(0);
        body.write(picture, 0, picture.length);
        frame(out, id, body.toByteArray());
    }

    private static void frame(java.io.ByteArrayOutputStream out, String id, byte[] body) {
        out.write(id.getBytes(), 0, 4);
        int size = body.length;
        out.write((size >>> 24) & 0xFF);
        out.write((size >>> 16) & 0xFF);
        out.write((size >>> 8) & 0xFF);
        out.write(size & 0xFF);
        out.write(0); // flags
        out.write(0);
        out.write(body, 0, body.length);
    }
}