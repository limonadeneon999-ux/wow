package app.morphe.extension.music.patches.downloads;

/**
 * Download format and quality choices exposed in the settings screen.
 *
 * <p>The format decides how {@link LocalDownloadManager} publishes a download:
 * <ul>
 *   <li>{@link DownloadFormat#OPUS} keeps the opus stream as Ogg Opus (smallest);
 *   <li>{@link DownloadFormat#MP3} transcodes to a true MP3 file (maximum compatibility);
 *   <li>{@link DownloadFormat#MP4} keeps the AAC stream in an MP4 container.
 * </ul>
 *
 * <p>{@link DownloadFormat#OPUS} and {@link DownloadFormat#MP4} copy the stream as-is, so the
 * file keeps the stream's own bitrate and {@link DownloadQuality} acts as a ceiling: the highest
 * offered rung that does not exceed it. {@link DownloadFormat#MP3} re-encodes with LAME at the
 * chosen bitrate, so it always downloads the best available source.
 */
public final class DownloadsConfig {

    public enum DownloadFormat {
        OPUS,
        MP3,
        MP4
    }

    /** MP3 quality, mapped to an LAME target bitrate. */
    public enum DownloadQuality {
        LOW(128),
        STANDARD(192),
        HIGH(320);

        public final int bitrateKbps;

        DownloadQuality(int bitrateKbps) {
            this.bitrateKbps = bitrateKbps;
        }
    }

    private DownloadsConfig() {
    }
}