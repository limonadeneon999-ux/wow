/*
 * Copyright 2026 Morphe.
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.downloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the container detection helpers of {@link AudioTagger}.
 *
 * <p>These decide whether a downloaded stream can be tagged in place and which
 * container the published file gets, so a wrong answer either produces an
 * untaggable file or the wrong MIME type in MediaStore.
 */
public class AudioTaggerTest {

    @Test
    public void isMp4AcceptsPlainMimeType() {
        assertTrue(AudioTagger.isMp4("audio/mp4"));
    }

    @Test
    public void isMp4AcceptsMimeTypeWithCodecsParameter() {
        assertTrue(AudioTagger.isMp4("audio/mp4; codecs=\"mp4a.40.2\""));
    }

    @Test
    public void isMp4RejectsWebmAndOgg() {
        assertFalse(AudioTagger.isMp4("audio/webm; codecs=\"opus\""));
        assertFalse(AudioTagger.isMp4("audio/ogg"));
    }

    @Test
    public void canTagMp4Streams() {
        assertTrue(AudioTagger.canTag("audio/mp4"));
        assertTrue(AudioTagger.canTag("audio/mp4; codecs=\"mp4a.40.2\""));
    }

    @Test
    public void canTagOpusInWebm() {
        assertTrue(AudioTagger.canTag("audio/webm; codecs=\"opus\""));
    }

    @Test
    public void cannotTagWebmWithNonOpusCodec() {
        assertFalse(AudioTagger.canTag("audio/webm; codecs=\"vorbis\""));
    }

    @Test
    public void cannotTagUnsupportedContainers() {
        assertFalse(AudioTagger.canTag("audio/ogg"));
        assertFalse(AudioTagger.canTag("audio/mpeg"));
    }

    @Test
    public void outputMimeTypeKeepsMp4Container() {
        assertEquals("audio/mp4", AudioTagger.outputMimeType("audio/mp4"));
    }

    @Test
    public void outputMimeTypeMapsWebmToOgg() {
        assertEquals("audio/ogg", AudioTagger.outputMimeType("audio/webm; codecs=\"opus\""));
    }

    @Test
    public void imageMimeTypeDetectsJpeg() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        assertEquals("image/jpeg", AudioTagger.imageMimeType(jpeg));
    }

    @Test
    public void imageMimeTypeDetectsPng() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        assertEquals("image/png", AudioTagger.imageMimeType(png));
    }

    @Test
    public void imageMimeTypeRejectsTruncatedJpeg() {
        // A JPEG signature needs more than three bytes to be recognised.
        byte[] truncated = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        assertNull(AudioTagger.imageMimeType(truncated));
    }

    @Test
    public void imageMimeTypeRejectsTruncatedPng() {
        // A PNG signature needs more than eight bytes to be recognised.
        byte[] truncated = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        assertNull(AudioTagger.imageMimeType(truncated));
    }

    @Test
    public void imageMimeTypeRejectsUnknownFormat() {
        byte[] webp = {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};
        assertNull(AudioTagger.imageMimeType(webp));
    }
}
