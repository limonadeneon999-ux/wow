/*
 * Copyright 2026 Morphe.
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.downloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the download format/quality choices of {@link DownloadsConfig}.
 *
 * <p>The enum constant names are persisted verbatim in the settings preference
 * (see the {@code morphe_music_download_format_entry_values} array), so renaming
 * a constant silently breaks existing user settings. The bitrates are the values
 * handed to LAME, so they are part of the output contract.
 */
public class DownloadsConfigTest {

    @Test
    public void formatConstantsKeepTheirPersistedNames() {
        assertEquals("OPUS", DownloadsConfig.DownloadFormat.OPUS.name());
        assertEquals("MP3", DownloadsConfig.DownloadFormat.MP3.name());
        assertEquals("MP4", DownloadsConfig.DownloadFormat.MP4.name());
    }

    @Test
    public void qualityConstantsKeepTheirPersistedNames() {
        assertEquals("LOW", DownloadsConfig.DownloadQuality.LOW.name());
        assertEquals("STANDARD", DownloadsConfig.DownloadQuality.STANDARD.name());
        assertEquals("HIGH", DownloadsConfig.DownloadQuality.HIGH.name());
    }

    @Test
    public void qualityMapsToExpectedLameBitrate() {
        assertEquals(128, DownloadsConfig.DownloadQuality.LOW.bitrateKbps);
        assertEquals(192, DownloadsConfig.DownloadQuality.STANDARD.bitrateKbps);
        assertEquals(320, DownloadsConfig.DownloadQuality.HIGH.bitrateKbps);
    }

    @Test
    public void formatCountMatchesEntryValuesArray() {
        // The settings XML array lists exactly OPUS, MP3, MP4.
        assertEquals(3, DownloadsConfig.DownloadFormat.values().length);
    }

    @Test
    public void qualityCountMatchesEntryValuesArray() {
        // The settings XML array lists exactly LOW, STANDARD, HIGH.
        assertEquals(3, DownloadsConfig.DownloadQuality.values().length);
    }

    @Test
    public void bitratesIncreaseWithQuality() {
        int low = DownloadsConfig.DownloadQuality.LOW.bitrateKbps;
        int standard = DownloadsConfig.DownloadQuality.STANDARD.bitrateKbps;
        int high = DownloadsConfig.DownloadQuality.HIGH.bitrateKbps;
        assertTrue(low < standard);
        assertTrue(standard < high);
    }

    @Test
    public void everyQualityHasAPositiveBitrate() {
        for (DownloadsConfig.DownloadQuality quality : DownloadsConfig.DownloadQuality.values()) {
            assertTrue(quality.name() + " must have a positive bitrate", quality.bitrateKbps > 0);
        }
    }
}
