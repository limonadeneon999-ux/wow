package app.morphe.extension.music.patches.downloads;

import android.content.Context;
import android.content.SharedPreferences;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Tracks how often each song is played and triggers automatic downloads for
 * the most-played ones.
 *
 * <p>Played songs are counted locally so automatic downloads work without any
 * server. When a song enters the top-N most played (N is the user-adjustable
 * {@link Settings#AUTO_DOWNLOAD_TOP_TRACKS_COUNT}), it is downloaded in the
 * background through {@link LocalDownloadManager}.
 */
public final class TopTracks {

    private static final String PREFS = "morphe_music_top_tracks";
    private static final String KEY_COUNT_PREFIX = "play_count_";
    private static final String KEY_TITLE_PREFIX = "play_title_";
    private static final String KEY_ARTIST_PREFIX = "play_artist_";

    private TopTracks() {
    }

    /** Records a play and auto-downloads the song if it enters the most-played top N. */
    public static void onTrackPlayed(String videoId, String title, String artist) {
        if (videoId == null || videoId.isEmpty() || !Settings.NATIVE_DOWNLOADS.get()) return;
        if (Settings.DOWNLOAD_FORMAT.get() == null
                || !Settings.AUTO_DOWNLOAD_TOP_TRACKS.get()) return;
        try {
            SharedPreferences prefs = prefs();
            int count = prefs.getInt(KEY_COUNT_PREFIX + videoId, 0) + 1;
            prefs.edit()
                    .putInt(KEY_COUNT_PREFIX + videoId, count)
                    .putString(KEY_TITLE_PREFIX + videoId, title)
                    .putString(KEY_ARTIST_PREFIX + videoId, artist)
                    .apply();
            maybeAutoDownload(prefs, videoId, count);
        } catch (Exception ex) {
            Logger.printException(() -> "TopTracks onTrackPlayed failure", ex);
        }
    }

    private static void maybeAutoDownload(SharedPreferences prefs, String videoId, int count) {
        int limit = Settings.AUTO_DOWNLOAD_TOP_TRACKS_COUNT.get();
        if (limit <= 0 || !isInTop(prefs, videoId, count, limit)) return;
        if (NativeAudioStore.find(videoId) != null) return;
        Logger.printDebug(() -> "Auto-downloading most-played " + videoId);
        LocalDownloadManager.enqueue(videoId);
    }

    /** A song is most-played when its count reaches the lowest count of the top N. */
    private static boolean isInTop(SharedPreferences prefs, String videoId, int count, int limit) {
        int lowest = Integer.MAX_VALUE;
        int entries = 0;
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(KEY_COUNT_PREFIX)) continue;
            int c = prefs.getInt(key, 0);
            entries++;
            if (c < lowest) lowest = c;
        }
        return count >= lowest || entries <= limit;
    }

    private static SharedPreferences prefs() {
        return Utils.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}