package app.morphe.extension.music.patches.downloads;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Tracks how often each song is played and triggers automatic downloads for
 * the most-played ones.
 *
 * <p>A song's score represents its recent popularity: each play adds a point
 * that halves over time ({@link #HALF_LIFE_MS}), so a song played a lot a long
 * time ago but not recently naturally leaves the top. Auto-download only runs
 * on Wi-Fi ({@link Settings#AUTO_DOWNLOAD_WIFI_ONLY}) and for the top-N
 * most-played songs (N = {@link Settings#AUTO_DOWNLOAD_TOP_TRACKS_COUNT}).
 */
public final class TopTracks {

    private static final String PREFS = "morphe_music_top_tracks";
    private static final String KEY_COUNT_PREFIX = "play_count_";
    private static final String KEY_LAST_PREFIX = "play_last_";
    private static final String KEY_TITLE_PREFIX = "play_title_";
    private static final String KEY_ARTIST_PREFIX = "play_artist_";

    /** Score halves after this time. 30 days keeps the top focused on current habits. */
    private static final long HALF_LIFE_MS = 30L * 24 * 60 * 60 * 1000;

    private TopTracks() {
    }

    /** Records a play and auto-downloads the song if it enters the most-played top N. */
    public static void onTrackPlayed(String videoId, String title, String artist) {
        if (videoId == null || videoId.isEmpty() || !Settings.NATIVE_DOWNLOADS.get()) return;
        if (Settings.DOWNLOAD_FORMAT.get() == null
                || !Settings.AUTO_DOWNLOAD_TOP_TRACKS.get()) return;
        try {
            long now = System.currentTimeMillis();
            SharedPreferences prefs = prefs();
            long last = prefs.getLong(KEY_LAST_PREFIX + videoId, now);
            double decayed = decay(prefs.getFloat(KEY_COUNT_PREFIX + videoId, 0f), last, now);
            double score = decayed + 1.0;
            prefs.edit()
                    .putFloat(KEY_COUNT_PREFIX + videoId, (float) score)
                    .putLong(KEY_LAST_PREFIX + videoId, now)
                    .putString(KEY_TITLE_PREFIX + videoId, title)
                    .putString(KEY_ARTIST_PREFIX + videoId, artist)
                    .apply();
            maybeAutoDownload(prefs, videoId, score);
        } catch (Exception ex) {
            Logger.printException(() -> "TopTracks onTrackPlayed failure", ex);
        }
    }

    private static void maybeAutoDownload(SharedPreferences prefs, String videoId, double score) {
        if (!isInTop(prefs, videoId, score)) return;
        if (Settings.AUTO_DOWNLOAD_WIFI_ONLY.get() && !isOnWifi()) {
            Logger.printDebug(() -> "Skipping auto-download: not on Wi-Fi");
            return;
        }
        Logger.printDebug(() -> "Auto-downloading most-played " + videoId);
        LocalDownloadManager.enqueueQuietly(videoId);
        autoDownloadCurrentTop(prefs);
    }

    /**
     * On each play, scan the whole current top and download every missing song.
     * Because scores decay over time this also refreshes the library on Wi-Fi
     * without needing new plays for old tracks.
     */
    private static void autoDownloadCurrentTop(SharedPreferences prefs) {
        int limit = Settings.AUTO_DOWNLOAD_TOP_TRACKS_COUNT.get();
        if (limit <= 0) return;
        long now = System.currentTimeMillis();
        java.util.List<java.util.Map.Entry<String, Double>> ranked = new java.util.ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(KEY_COUNT_PREFIX)) continue;
            String id = key.substring(KEY_COUNT_PREFIX.length());
            long last = prefs.getLong(KEY_LAST_PREFIX + id, 0L);
            double s = decay(prefs.getFloat(key, 0f), last, now);
            if (s > 0) ranked.add(new java.util.AbstractMap.SimpleEntry<>(id, s));
        }
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        int downloaded = 0;
        for (java.util.Map.Entry<String, Double> entry : ranked) {
            if (downloaded >= limit) break;
            String id = entry.getKey();
            if (NativeAudioStore.find(id) != null) continue;
            LocalDownloadManager.enqueueQuietly(id);
            downloaded++;
        }
    }

    /** A song is most-played while its current score beats the lowest top-score. */
    private static boolean isInTop(SharedPreferences prefs, String videoId, double score) {
        int limit = Settings.AUTO_DOWNLOAD_TOP_TRACKS_COUNT.get();
        if (limit <= 0) return false;
        long now = System.currentTimeMillis();
        double lowest = Double.MAX_VALUE;
        int entries = 0;
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(KEY_COUNT_PREFIX) || key.equals(KEY_COUNT_PREFIX + videoId)) continue;
            long last = prefs.getLong(KEY_LAST_PREFIX + key.substring(KEY_COUNT_PREFIX.length()), 0L);
            double s = decay(prefs.getFloat(key, 0f), last, now);
            if (s > 0) entries++;
            if (s < lowest) lowest = s;
        }
        return entries < limit || score >= lowest;
    }

    /** Exponential decay: score halves every {@link #HALF_LIFE_MS}. */
    private static double decay(float score, long lastPlayed, long now) {
        if (score <= 0f || lastPlayed <= 0) return 0.0;
        long age = Math.max(0L, now - lastPlayed);
        return score * Math.pow(0.5, (double) age / HALF_LIFE_MS);
    }

    /** True when the device is connected to a Wi-Fi network with internet. */
    private static boolean isOnWifi() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    Utils.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Exception ex) {
            Logger.printException(() -> "TopTracks WiFi check failure", ex);
            return false;
        }
    }

    private static SharedPreferences prefs() {
        return Utils.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}