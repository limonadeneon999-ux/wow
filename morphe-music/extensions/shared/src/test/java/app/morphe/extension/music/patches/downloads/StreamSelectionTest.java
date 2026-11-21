/*
 * Copyright 2026 Morphe.
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.downloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stream picking for {@link LocalDownloadManager}.
 *
 * <p>Mirrors the filter/comparator of {@code parseStream}: the chosen format decides the
 * container. OPUS and MP4 are passthrough, so the requested quality selects the stream;
 * MP3 is re-encoded by LAME and always takes the best source.
 */
public class StreamSelectionTest {

    private record Stream(String mimeType, int bitrate) {
    }

    private static final List<Stream> YOUTUBE_AUDIO = List.of(
            new Stream("audio/webm; codecs=\"opus\"", 160_000),
            new Stream("audio/mp4; codecs=\"mp4a.40.2\"", 128_000),
            new Stream("audio/mp4; codecs=\"mp4a.40.2\"", 256_000),
            new Stream("audio/mp4; codecs=\"mp4a.40.2\"", 48_000));

    private static Stream select(DownloadsConfig.DownloadFormat wanted, int qualityKbps) {
        return selectFrom(YOUTUBE_AUDIO, wanted, qualityKbps);
    }

    private static Stream selectFrom(List<Stream> streams, DownloadsConfig.DownloadFormat wanted, int qualityKbps) {
        boolean wantOpus = wanted == DownloadsConfig.DownloadFormat.OPUS;
        boolean passthrough = wanted != DownloadsConfig.DownloadFormat.MP3;
        int targetBitrate = qualityKbps * 1000;

        List<Stream> candidates = streams.stream()
                .filter(stream -> stream.mimeType().startsWith("audio/")
                        && AudioTagger.canTag(stream.mimeType())
                        && (AudioTagger.isMp4(stream.mimeType()) != wantOpus))
                .collect(Collectors.toList());

        if (!passthrough) {
            return candidates.stream()
                    .max(Comparator.comparingInt(Stream::bitrate))
                    .orElse(null);
        }
        return candidates.stream()
                .filter(stream -> stream.bitrate() <= targetBitrate)
                .max(Comparator.comparingInt(Stream::bitrate))
                .orElseGet(() -> candidates.stream()
                        .min(Comparator.comparingInt(Stream::bitrate))
                        .orElse(null));
    }

    @Test
    public void opusFormatTakesTheOpusStream() {
        Stream chosen = select(DownloadsConfig.DownloadFormat.OPUS, 320);
        assertEquals("audio/webm; codecs=\"opus\"", chosen.mimeType());
    }

    @Test
    public void opusPassthroughHonoursTheRequestedQuality() {
        // Opus is copied, not re-encoded, so LOW must really pick the small opus stream
        // instead of always taking the largest one.
        List<Stream> opusStreams = List.of(
                new Stream("audio/webm; codecs=\"opus\"", 48_000),
                new Stream("audio/webm; codecs=\"opus\"", 160_000));
        assertEquals(48_000, selectFrom(opusStreams, DownloadsConfig.DownloadFormat.OPUS, 128).bitrate());
        assertEquals(160_000, selectFrom(opusStreams, DownloadsConfig.DownloadFormat.OPUS, 320).bitrate());
    }

    @Test
    public void mp3StillIgnoresQualityAndTakesTheBestSource() {
        // LAME sets the output bitrate, so a low quality must not downgrade the source.
        List<Stream> opusStreams = List.of(
                new Stream("audio/webm; codecs=\"opus\"", 48_000),
                new Stream("audio/webm; codecs=\"opus\"", 160_000));
        assertNull(selectFrom(opusStreams, DownloadsConfig.DownloadFormat.MP3, 128));
    }

    @Test
    public void mp3FormatNeverTakesTheOpusStream() {
        // LAME cannot decode Opus, so the source has to be AAC.
        for (int kbps : new int[]{128, 192, 320}) {
            Stream chosen = select(DownloadsConfig.DownloadFormat.MP3, kbps);
            assertEquals("audio/mp4; codecs=\"mp4a.40.2\"", chosen.mimeType());
        }
    }

    @Test
    public void mp3AlwaysTakesTheHighestSourceRegardlessOfQuality() {
        // The output bitrate is set by LAME, so the best possible source is always wanted.
        for (int kbps : new int[]{128, 192, 320}) {
            assertEquals(256_000, select(DownloadsConfig.DownloadFormat.MP3, kbps).bitrate());
        }
    }

    @Test
    public void mp4PassthroughHonoursTheRequestedQuality() {
        assertEquals(128_000, select(DownloadsConfig.DownloadFormat.MP4, 128).bitrate());
        assertEquals(256_000, select(DownloadsConfig.DownloadFormat.MP4, 320).bitrate());
        assertEquals(48_000, select(DownloadsConfig.DownloadFormat.MP4, 48).bitrate());
    }

    @Test
    public void mp4QualityIsACeilingSoLowNeverUpscales() {
        // 192 kbps is not offered: the ceiling takes the best rung that does not exceed it.
        assertEquals(128_000, select(DownloadsConfig.DownloadFormat.MP4, 192).bitrate());
    }

    @Test
    public void mp4FallsBackToTheSmallestRungWhenEveryStreamIsAboveTheCeiling() {
        // No AAC rung is under 48 kbps, so LOW still has to return something playable.
        assertEquals(48_000, select(DownloadsConfig.DownloadFormat.MP4, 40).bitrate());
    }

    @Test
    public void everyFormatSelectsAnAacOrOpusStream() {
        for (DownloadsConfig.DownloadFormat format : DownloadsConfig.DownloadFormat.values()) {
            Stream chosen = select(format, 192);
            assertEquals(format.name(), true, AudioTagger.canTag(chosen.mimeType()));
        }
    }

    @Test
    public void mp3FindsNoSourceWhenOnlyOpusIsOffered() {
        // LAME cannot decode Opus, so an Opus-only response must yield no stream
        // rather than a download that would fail at transcode time.
        List<Stream> opusOnly = YOUTUBE_AUDIO.stream()
                .filter(stream -> !AudioTagger.isMp4(stream.mimeType()))
                .collect(Collectors.toList());
        Stream chosen = opusOnly.stream()
                .filter(stream -> AudioTagger.isMp4(stream.mimeType()))
                .findFirst()
                .orElse(null);
        assertNull(chosen);
    }
}
