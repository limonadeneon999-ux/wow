/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2528
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 *
 * Modified by lavinhoque33, 2026-10-21.
 */

package app.morphe.extension.music.patches.downloads;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import app.morphe.extension.music.patches.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.VideoInformation;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.Format;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerResponse;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.VideoDetails;
import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.spoof.requests.StreamOrDetailsDataRequest;

/** Downloads the already resolved InnerTube audio stream without leaving YouTube Music. */
public final class LocalDownloadManager {

    private static final String DOWNLOAD_CHANNEL = "morphe_audio_downloads";
    private static final String USER_AGENT =
            "com.google.android.youtube/19.47.53 (Linux; U; Android 14) gzip";

    /**
     * Ranges are fetched in parallel, which is what makes a download finish in seconds.
     * Short files are still split into this many equal ranges.
     */
    private static final int SEGMENT_COUNT = 4;
    // googlevideo throttles any single range response
    // above ~10 MiB (16 MiB ranges ran at ~3 MB/s), so ranges are bounded and handed out from a queue.
    private static final long MAX_RANGE_SIZE = 8L * 1024 * 1024;
    private static final int MAX_CONNECTIONS = 6;
    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * The system silently drops notifications posted faster than a few per second, which used to
     * leave stale progress on screen after the download had finished. Updates are spaced out.
     */
    private static final long NOTIFICATION_INTERVAL_MILLISECONDS = 1000;
    private static final AtomicLong lastNotificationTime = new AtomicLong();

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 15_000;
    private static final int READ_TIMEOUT_MILLISECONDS = 30_000;

    /** A range that makes no progress this many times in a row is treated as stalled. */
    private static final int MAX_STALLED_ATTEMPTS = 4;

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    // song and playlist requests share one result for each active media download.
    private static final ConcurrentHashMap<String, FutureTask<Uri>> ACTIVE_DOWNLOADS = new ConcurrentHashMap<>();

    /**
     * Ids already queued or running. ACTIVE_DOWNLOADS is only populated once the task starts
     * executing, so a catch-up that runs while the queue is busy would enqueue the same track
     * again for every pass.
     */
    private static final Set<String> PENDING_DOWNLOADS = ConcurrentHashMap.newKeySet();

    private static volatile String currentTitle;
    private static volatile String currentArtist;

    /**
     * Last song already scored and when, so repeat metadata updates for one song count as one
     * play while replaying the same song later still counts. A bare id is not enough: a user
     * looping one track would never score a second play.
     */
    private static volatile String lastScoredVideoId;
    private static volatile long lastScoredAt;

    /**
     * A republication of the same song's metadata arrives within a second or so; a real replay
     * takes longer than this to restart. Only touched on {@link #PLAY_SCORING_THREAD}.
     */
    private static final long REPEAT_PLAY_WINDOW_MILLISECONDS = 15_000;

    /**
     * Single thread so two rapid track changes cannot interleave their read-modify-write
     * of the play counters, which would lose an increment.
     */
    private static final ExecutorService PLAY_SCORING_THREAD = Executors.newSingleThreadExecutor();

    /**
     * Auto-downloads run one at a time. Each one already opens {@link #MAX_CONNECTIONS}
     * sockets and an encoder, so running the whole top-N list in parallel is what melts
     * the device. A manual download is unaffected and uses the shared pool.
     */
    private static final ExecutorService AUTO_DOWNLOAD_THREAD = Executors.newSingleThreadExecutor();

    /**
     * Temporary files a download is currently writing. The orphan sweep must not touch these:
     * auto-downloads are serialised but a manual one runs on the shared pool at the same time,
     * so a sweep that deleted every matching file would destroy a live transfer.
     */
    private static final Set<File> LIVE_TEMPORARY_FILES = ConcurrentHashMap.newKeySet();

    private static final java.util.concurrent.atomic.AtomicBoolean SWEPT_ORPHANS = new java.util.concurrent.atomic.AtomicBoolean();

    /** Guards the create-then-register pair, which must be atomic against the sweep. */
    private static final Object TEMPORARY_FILE_LOCK = new Object();

    /**
     * Creates a temp file and registers it as live before it becomes visible to the sweep.
     * Creating it first and registering after leaves a window where the file exists on disk
     * without being claimed, and a concurrent sweep would delete it.
     */
    private static File createLiveTemporaryFile() throws java.io.IOException {
        synchronized (TEMPORARY_FILE_LOCK) {
            File file = File.createTempFile("rvx-audio-", ".part", Utils.getContext().getCacheDir());
            LIVE_TEMPORARY_FILES.add(file);
            return file;
        }
    }

    public static void onSetMetadata(android.media.MediaMetadata metadata) {
        if (metadata == null) return;
        currentTitle = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
        currentArtist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST);
        // Runs on the main thread for every track change, while scoring a play touches
        // SharedPreferences plus one openFileDescriptor per ranked song.
        final String title = currentTitle;
        final String artist = currentArtist;
        final String videoId = app.morphe.extension.music.shared.VideoInformation.getVideoId();
        // MediaSession republishes metadata several times per song (title first, then
        // artwork, then duration). Scoring each update would multiply one play by three, but
        // ignoring every repeat of an id would mean a looped track never scores again, so the
        // decision is made on the scoring thread where it can also be atomic.
        if (videoId == null) return;
        PLAY_SCORING_THREAD.execute(() -> {
            long now = System.currentTimeMillis();
            if (!shouldScorePlay(lastScoredVideoId, lastScoredAt, videoId, now)) return;
            lastScoredVideoId = videoId;
            lastScoredAt = now;
            TopTracks.onTrackPlayed(videoId, title, artist);
        });
    }

    /**
     * MediaSession republishes a song's metadata within a second or so, but a replay restarts
     * later. Only repeats inside {@link #REPEAT_PLAY_WINDOW_MILLISECONDS} are treated as
     * republication; anything later is a new play, so looping one track still counts.
     */
    static boolean shouldScorePlay(String previousId, long previousAt, String videoId, long now) {
        if (videoId == null) return false;
        return !videoId.equals(previousId) || now - previousAt >= REPEAT_PLAY_WINDOW_MILLISECONDS;
    }

    private LocalDownloadManager() {
    }

    /** Must be called from the main thread. Resolution and downloading happen in background. */
    public static void enqueue(String videoId) {
        enqueue(videoId, false);
    }

    /** Background auto-download: no toasts, silently ignores already-saved or running tracks. */
    static void enqueueQuietly(String videoId) {
        enqueue(videoId, true);
    }

    private static void enqueue(String videoId, boolean quiet) {
        if (!VIDEO_ID.matcher(videoId).matches()) {
            if (!quiet) Utils.showToastShort(str("morphe_music_downloads_unknown_track"));
            return;
        }
        // A manual download starts on the main thread and may prompt for storage; a quiet
        // auto-download can start from any thread and must only check.
        if (quiet ? !NativeDownloads.hasStoragePermission() : !NativeDownloads.ensureStoragePermission()) return;
        if (ACTIVE_DOWNLOADS.containsKey(videoId)) return;
        // Reserved before the task is submitted: the check above cannot see queued work.
        if (!PENDING_DOWNLOADS.add(videoId)) return;
        // An auto-download catch-up can queue hundreds of songs. Each one opens up to
        // MAX_CONNECTIONS sockets and a LAME/MediaCodec encode, so running them on the
        // unbounded shared pool exhausts memory and gets the account throttled by 403/429.
        // Manual downloads keep the shared pool: the user asked for exactly one.
        if (quiet) {
            submitOrRelease(videoId, () -> {
                runDownload(videoId, true);
                return null;
            }, true);
        } else {
            submitOrRelease(videoId, () -> {
                runDownload(videoId, false);
                return null;
            }, false);
        }
    }

    /**
     * A rejected submission would otherwise strand the reservation, and the track could never be
     * downloaded again for the life of the process.
     */
    private static void submitOrRelease(String videoId, java.util.concurrent.Callable<Void> task, boolean quiet) {
        try {
            if (quiet) {
                AUTO_DOWNLOAD_THREAD.submit(task);
            } else {
                Utils.submitOnBackgroundThread(task);
            }
        } catch (RuntimeException ex) {
            PENDING_DOWNLOADS.remove(videoId);
            throw ex;
        }
    }

    private static void runDownload(String videoId, boolean quiet) {
        try {
            NativeDownloads.migrateLegacy();
            sweepOrphanedTemporaryFiles();
            if (NativeAudioStore.find(videoId) != null) return;
            if (!quiet) Utils.showToastShort(str("morphe_music_downloads_started"));
            // `quiet` only says whether toasts are wanted; the Wi-Fi requirement is the user's
            // setting. Passing `quiet` here made every auto-download Wi-Fi-only even with the
            // option turned off.
            download(videoId, quiet && Settings.AUTO_DOWNLOAD_WIFI_ONLY.get());
            if (!quiet) Utils.showToastShort(str("morphe_music_downloads_complete"));
            NativePlaylistStore.refreshUnsortedLater(); // list the new single.
        } catch (Exception ex) {
            Logger.printException(() -> "Local audio download failed: " + videoId, ex);
            if (!quiet) Utils.showToastShort(str("morphe_music_downloads_failed"));
        } finally {
            // Released here, not in enqueue: the id is reserved for the whole run.
            PENDING_DOWNLOADS.remove(videoId);
        }
    }

    static Uri download(String videoId) throws Exception {
        return download(videoId, false);
    }

    /**
     * @param requireWifi re-check the Wi-Fi-only setting before every network request, so a
     *                    queue that outlives the Wi-Fi connection stops instead of switching
     *                    to mobile data.
     */
    static Uri download(String videoId, boolean requireWifi) throws Exception {
        Utils.verifyOffMainThread();
        if (!VIDEO_ID.matcher(videoId).matches()) throw new IOException("Invalid video id");
        Uri saved = NativeAudioStore.find(videoId);
        if (saved != null) return saved;
        boolean playing = videoId.equals(VideoInformation.getVideoId());
        String title = playing ? currentTitle : null;
        String artist = playing ? currentArtist : null;
        FutureTask<Uri> task = new FutureTask<>(
                () -> downloadBlocking(videoId, title, artist, requireWifi));
        FutureTask<Uri> existing = ACTIVE_DOWNLOADS.putIfAbsent(videoId, task);
        if (existing == null) task.run();
        try {
            return (existing == null ? task : existing).get();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof Exception cause) throw cause;
            throw ex;
        } finally {
            if (existing == null) ACTIVE_DOWNLOADS.remove(videoId, task);
        }
    }

    /**
     * Deletes temporary files left behind by a crash or a system kill. Ordinary failures clean
     * up in a finally block, but a killed process leaves a full-size preallocated .part file in
     * the cache directory forever.
     *
     * <p>Runs at most once per process, and never removes a file a download is still writing.
     * Two downloads can be live at once (one manual on the shared pool, one automatic on
     * {@link #AUTO_DOWNLOAD_THREAD}), so a blind sweep would delete a live transfer.
     */
    private static void sweepOrphanedTemporaryFiles() {
        if (!SWEPT_ORPHANS.compareAndSet(false, true)) return;
        try {
            File[] candidates;
            // Held against createLiveTemporaryFile so a file is either claimed or swept,
            // never caught in between.
            synchronized (TEMPORARY_FILE_LOCK) {
                candidates = Utils.getContext().getCacheDir()
                        .listFiles((dir, name) -> name.startsWith("rvx-audio-") && name.endsWith(".part"));
                if (candidates == null) return;
                for (File orphan : candidates) {
                    if (LIVE_TEMPORARY_FILES.contains(orphan)) continue;
                    if (orphan.delete()) {
                        Logger.printDebug(() -> "Deleted orphaned temporary file: " + orphan.getName());
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printDebug(() -> "Could not sweep temporary files", ex);
        }
    }

    // the stream is downloaded to a private temporary file and
    // published with embedded tags, since Music's device-files library reads metadata only from the file.
    private static Uri downloadBlocking(String videoId, String title, String artist,
                                        boolean requireWifi) throws Exception {
        NativeAudioStore.Destination destination = null;
        File temporary = null;
        try {
            // A competing request may have published the track between the lookup and single-flight ownership.
            Uri saved = NativeAudioStore.find(videoId);
            if (saved != null) return saved;
            // Stream resolution is an InnerTube request, so it needs the gate too, not just the
            // audio transfer that follows it.
            requireNetwork(requireWifi);
            StreamCandidates candidates = new StreamCandidates(videoId);
            ResolvedStream resolved = candidates.next();
            if (resolved == null) throw new IOException("Audio stream unavailable");
            AudioTagger.Tags tags = TrackMetadata.resolve(videoId, title, artist, resolved.details());
            title = tags.title;
            awaitNotificationSlot();
            showDownloadNotification(videoId, title, 0, true, str("morphe_music_downloads_starting"));
            Format format;
            while (true) {
                format = resolved.format();
                temporary = createLiveTemporaryFile();
                try {
                    // Keep the existing bounded parallel range/resume downloader.
                    downloadWithResume(videoId, title, format.getUrl(),
                            new NativeAudioStore.Destination(null, temporary, format.getMimeType()),
                            format.getContentLength(), requireWifi);
                    break;
                } catch (ForbiddenException ex) {
                    LIVE_TEMPORARY_FILES.remove(temporary);
                    temporary.delete();
                    temporary = null;
                    // Asking another client for a url is another request.
                    requireNetwork(requireWifi);
                    resolved = candidates.next();
                    if (resolved == null) throw ex;
                }
            }
            // Pending MediaStore files are hidden from native Library until the full audio has been indexed.
            boolean mp3 = Settings.DOWNLOAD_FORMAT.get() == DownloadsConfig.DownloadFormat.MP3;
            String outputMimeType = mp3 ? "audio/mpeg" : AudioTagger.outputMimeType(format.getMimeType());
            destination = NativeAudioStore.create(videoId, outputMimeType, tags.title, tags.artist);
            try (FileOutputStream output = destination.openOutput()) {
                if (mp3) {
                    Mp3Transcoder.write(temporary, Settings.DOWNLOAD_QUALITY.get().bitrateKbps, output, tags);
                } else {
                    AudioTagger.write(temporary, format.getMimeType(), output, tags);
                }
            }
            Uri uri = NativeAudioStore.finish(videoId, destination);
            destination = null;
            awaitNotificationSlot();
            showDownloadNotification(videoId, title, 100, false, str("morphe_music_downloads_complete"));
            return uri;
        } catch (Exception ex) {
            awaitNotificationSlot();
            showDownloadNotification(videoId, title, 0, false, str("morphe_music_downloads_failed"));
            throw ex;
        } finally {
            // abort() can throw (it touches MediaStore on some paths); a throw here would skip
            // the temp file release below and leave it claimed forever.
            if (destination != null) {
                try {
                    NativeAudioStore.abort(destination);
                } catch (Exception ex) {
                    Logger.printDebug(() -> "Could not abort incomplete audio", ex);
                }
            }
            if (temporary != null) {
                final File leftover = temporary;
                LIVE_TEMPORARY_FILES.remove(leftover);
                if (!leftover.delete()) {
                    Logger.printDebug(() -> "Could not delete temporary file: " + leftover.getName());
                }
            }
        }
    }

    private record ResolvedStream(Format format, VideoDetails details) {
    }

    /** The server refused the stream's url itself, so another client's url may still work. */
    private static final class ForbiddenException extends IllegalStateException {
        ForbiddenException(String message) {
            super(message);
        }
    }

    /**
     * Hands out the audio streams a download may try, one resolved client at a time and in
     * playback order, so a refused stream costs only the next client's request.
     */
    private static final class StreamCandidates {
        private final String videoId;
        private final Iterator<ClientType> clients;

        StreamCandidates(String videoId) {
            this.videoId = videoId;
            Logger.printDebug(() -> "Resolving stream and metadata for download: " + videoId);
            // The clients are named here because the playback order is only set up while spoofing
            // is on, and a download needs a client that answers with plain urls either way.
            this.clients = StreamOrDetailsDataRequest.buildClientOrder(
                    SpoofVideoStreamsPatch.getAvailableClients(),
                    Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get()).iterator();
        }

        /** @return The next stream with an audio url, or null once every client has been tried. */
        @Nullable
        ResolvedStream next() {
            while (clients.hasNext()) {
                // Cached playback responses omit videoDetails, so fetch streams and song details together.
                ClientType client = clients.next();
                StreamOrDetailsDataRequest request = StreamOrDetailsDataRequest.fetchRequestForDownload(
                        videoId, List.of(client), client);

                ResolvedStream stream = parseStream(videoId, request);
                if (stream != null) return stream;
            }
            return null;
        }
    }

    /** @return Whether a progress update may be posted without the system dropping it. */
    static boolean canPostProgress() { // shared with playlist progress.
        long now = System.currentTimeMillis();
        long previous = lastNotificationTime.get();
        return now - previous >= NOTIFICATION_INTERVAL_MILLISECONDS
                && lastNotificationTime.compareAndSet(previous, now);
    }

    /**
     * Waits for the next slot, so a notification that matters is never the one being dropped.
     * The loop sleeps exactly as long as the slot is away, and repeats only if another thread
     * took the slot first. Must be called off the main thread.
     */
    static void awaitNotificationSlot() {
        while (true) {
            long now = System.currentTimeMillis();
            long previous = lastNotificationTime.get();
            // Clamped: a wall-clock jump backwards would otherwise make this sleep for hours.
            long remaining = Math.min(NOTIFICATION_INTERVAL_MILLISECONDS,
                    NOTIFICATION_INTERVAL_MILLISECONDS - (now - previous));

            if (remaining <= 0) {
                if (lastNotificationTime.compareAndSet(previous, now)) return;
                continue;
            }

            try {
                //noinspection BusyWait
                Thread.sleep(remaining);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


    private static void downloadWithResume(String videoId, String title, String url,
                                           NativeAudioStore.Destination temporary, long expectedLength,
                                           boolean requireWifi) throws Exception {
        if (expectedLength <= 0) {
            // The probe is a real network request, so it needs the same Wi-Fi gate as the
            // transfer it is about to size up.
            requireNetwork(requireWifi);
            expectedLength = probeContentLength(url);
        }
        final long totalLength = expectedLength;

        if (totalLength <= 0) {
            downloadUnknownLength(url, temporary, requireWifi);
        } else {
            downloadInParallel(videoId, title, url, temporary, totalLength, requireWifi);
        }

        if (totalLength > 0) {
            long actualLength = temporary.size();
            if (actualLength != totalLength) {
                throw new IllegalStateException("Incomplete audio file: " + actualLength + "/" + totalLength);
            }
        }
    }

    // bounded ranges are handed out from a shared queue to a
    // capped worker pool; the first failure stops every connection immediately.
    private static void downloadInParallel(String videoId, String title, String url,
                                           NativeAudioStore.Destination temporary, long totalLength,
                                           boolean requireWifi) throws Exception {
        temporary.setLength(totalLength);

        AtomicLong downloaded = new AtomicLong();
        ProgressCallback progress = bytes -> {
            if (!canPostProgress()) return;
            int percent = (int) Math.min(99, bytes * 100 / totalLength);
            showDownloadNotification(videoId, title, percent, true,
                    str("morphe_music_downloads_progress", percent));
        };

        // Short files keep the original equal split, longer ones use ranges below the throttle.
        long rangeSize = Math.min(MAX_RANGE_SIZE, (totalLength + SEGMENT_COUNT - 1) / SEGMENT_COUNT);
        long rangeCount = (totalLength + rangeSize - 1) / rangeSize;
        int workers = (int) Math.min(MAX_CONNECTIONS, rangeCount);
        RangeQueue queue = new RangeQueue(url, temporary, totalLength, rangeSize, downloaded,
                progress, requireWifi);

        // Android's ExecutorService is not AutoCloseable, so it is shut down in the finally below.
        //noinspection resource
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        ExecutorCompletionService<Void> completion = new ExecutorCompletionService<>(executor);

        try {
            for (int index = 0; index < workers; index++) {
                completion.submit(queue::work);
            }

            for (int index = 0; index < workers; index++) {
                try {
                    completion.take().get();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof Exception cause) throw cause;
                    throw ex;
                }
            }
        } finally {
            // No worker may still write to the file once this returns.
            queue.stop();
            executor.shutdownNow();
            boolean interrupted = false;
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private interface ProgressCallback {
        void update(long bytes);
    }

    /** Hands out consecutive ranges of one file to the workers that download them. */
    private static final class RangeQueue {
        private final String url;
        private final NativeAudioStore.Destination destination;
        private final long totalLength;
        private final long rangeSize;
        private final AtomicLong downloaded;
        private final ProgressCallback progress;
        private final AtomicLong nextStart = new AtomicLong();
        private final Set<HttpURLConnection> connections = ConcurrentHashMap.newKeySet();
        private final boolean requireWifi;
        private volatile boolean stopped;

        RangeQueue(String url, NativeAudioStore.Destination destination, long totalLength, long rangeSize,
                   AtomicLong downloaded, ProgressCallback progress, boolean requireWifi) {
            this.url = url;
            this.destination = destination;
            this.totalLength = totalLength;
            this.rangeSize = rangeSize;
            this.downloaded = downloaded;
            this.progress = progress;
            this.requireWifi = requireWifi;
        }

        /** Aborts every connection, so blocked reads fail at once instead of timing out. */
        void stop() {
            stopped = true;
            for (HttpURLConnection connection : connections) connection.disconnect();
        }

        Void work() throws Exception {
            // each worker owns a seekable provider
            // descriptor; writes retain the existing reusable byte buffer without an extra copy.
            try (FileOutputStream output = destination.openOutput()) {
                FileChannel channel = output.getChannel();
                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    long start = nextStart.getAndAdd(rangeSize);
                    if (start >= totalLength) return null;

                    downloadRange(start, Math.min(totalLength - 1, start + rangeSize - 1),
                            output, channel, buffer);
                }
            }
        }

        private void checkStopped() {
            if (stopped) throw new CancellationException("Download stopped");
            // Checked per range rather than once at enqueue time: a long download can outlive
            // the Wi-Fi connection, and switching to mobile data is exactly what the setting
            // exists to prevent.
            if (requireWifi && !TopTracks.isOnWifi()) {
                stop();
                throw new CancellationException("Wi-Fi connection lost");
            }
        }

        private void downloadRange(long start, long end, FileOutputStream output,
                                   FileChannel channel, byte[] buffer) throws Exception {
            long position = start;
            int failures = 0;

            while (position <= end) {
                checkStopped();
                long before = position;
                HttpURLConnection connection = openAudioConnection(url, position, end);
                boolean reusable = false;
                connections.add(connection);

                try {
                    checkStopped();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        throw new ForbiddenException("Range request returned HTTP 403");
                    }
                    if (responseCode != HttpURLConnection.HTTP_PARTIAL) {
                        throw new IllegalStateException("Range request returned HTTP " + responseCode);
                    }

                    try (InputStream input = connection.getInputStream()) {
                        channel.position(position);

                        while (position <= end) {
                            int read = readBuffer(input, buffer,
                                    (int) Math.min(buffer.length, end - position + 1));
                            if (read == 0) break;

                            output.write(buffer, 0, read);
                            position += read;
                            progress.update(downloaded.addAndGet(read));
                        }

                        // Reading up to the end lets Android keep the connection for the next range.
                        reusable = position > end && input.read() < 0;
                    }
                } catch (IOException ex) {
                    checkStopped();
                    Logger.printDebug(() -> "Audio range interrupted; retrying", ex);
                } finally {
                    connections.remove(connection);
                    if (!reusable) connection.disconnect();
                }

                failures = position > before ? 0 : failures + 1;
                if (failures >= MAX_STALLED_ATTEMPTS) {
                    throw new IllegalStateException("Download range stalled at " + position);
                }
            }
        }

        // Android returns small socket reads, so fill the
        // buffer before each file write instead of writing every few KiB.
        private int readBuffer(InputStream input, byte[] buffer, int length) throws IOException {
            int filled = 0;
            while (filled < length) {
                checkStopped();
                int read = input.read(buffer, filled, length - filled);
                if (read < 0) break;
                filled += read;
            }
            return filled;
        }
    }

    /**
     * Gate for every network request a download makes. Checking the network and then opening a
     * connection leaves a small window, but it stops a queue that outlived the Wi-Fi from
     * quietly continuing over mobile data.
     */
    private static void requireNetwork(boolean requireWifi) {
        if (requireWifi && !TopTracks.isOnWifi()) {
            throw new CancellationException("Wi-Fi connection lost");
        }
    }

    private static long probeContentLength(String url) {
        HttpURLConnection connection = null;
        try {
            connection = openAudioConnection(url, 0, 0);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) return -1;

            String contentRange = connection.getHeaderField("Content-Range");
            if (contentRange == null) return -1;

            int slash = contentRange.lastIndexOf('/');
            return slash < 0 ? -1 : Long.parseLong(contentRange.substring(slash + 1));
        } catch (Exception ex) {
            Logger.printDebug(() -> "Could not probe audio length", ex);
            return -1;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static HttpURLConnection openAudioConnection(String url, long start, long end) throws Exception {
        HttpURLConnection connection = Requester.openConnection(url);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", "https://music.youtube.com/");
        connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
        return connection;
    }

    private static void downloadUnknownLength(String url, NativeAudioStore.Destination destination,
                                              boolean requireWifi) throws Exception {
        requireNetwork(requireWifi);
        HttpURLConnection connection = Requester.openConnection(url);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", "https://music.youtube.com/");

        try {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new ForbiddenException("HTTP 403");
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }

            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = destination.openOutput()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                long copied = 0;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    copied += read;
                    // A single check at the top only covers the connection setup; a long
                    // transfer has to notice the Wi-Fi going away as well.
                    if (requireWifi && copied >= BUFFER_SIZE * 64) {
                        copied = 0;
                        requireNetwork(true);
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Segments report progress from their own threads, and {@code notify} is a oneway binder
     * call whose order only holds per sending thread. Posting every update from the main thread
     * stops late progress from replacing the completed state on screen.
     */
    static void showDownloadNotification(String key, String title, int progress,
                                         boolean ongoing, String text) {
        Utils.runOnMainThread(() -> postDownloadNotification(key, title, progress, ongoing, text));
    }

    @SuppressLint("NotificationPermission") // The patch declares POST_NOTIFICATIONS.
    private static void postDownloadNotification(String key, String title, int progress,
                                                 boolean ongoing, String text) {
        Context context = Utils.getContext();
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        manager.createNotificationChannel(new NotificationChannel(DOWNLOAD_CHANNEL,
                str("morphe_music_downloads_notification_channel"),
                NotificationManager.IMPORTANCE_LOW));

        Notification.Builder builder = new Notification.Builder(context, DOWNLOAD_CHANNEL);
        builder.setSmallIcon(ongoing
                        ? android.R.drawable.stat_sys_download
                        : android.R.drawable.stat_sys_download_done)
                .setContentTitle(title == null || title.isBlank() ? "YouTube Music" : title)
                .setContentText(text)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true);

        if (ongoing) builder.setProgress(100, Math.max(0, progress), progress < 0);
        manager.notify(key.hashCode(), builder.build());
    }

    @Nullable
    private static ResolvedStream parseStream(String videoId,
                                              StreamOrDetailsDataRequest request) {
        StreamOrDetailsDataRequest.StreamData stream = request.getStream();
        if (stream == null) {
            Logger.printDebug(() -> "No stream data resolved for " + videoId);
            return null;
        }

        try {
            PlayerResponse response = PlayerResponse.parseFrom(stream.streamingData());
            // A missing enum (older patched install) must not break downloading.
            DownloadsConfig.DownloadFormat wanted = Settings.DOWNLOAD_FORMAT.get();
            if (wanted == null) wanted = DownloadsConfig.DownloadFormat.MP3;
            DownloadsConfig.DownloadQuality quality = Settings.DOWNLOAD_QUALITY.get();
            if (quality == null) quality = DownloadsConfig.DownloadQuality.STANDARD;
            // OPUS keeps the WebM/Opus stream; MP3 and MP4 are both cut from the AAC stream.
            final boolean wantOpus = wanted == DownloadsConfig.DownloadFormat.OPUS;
            // OPUS and MP4 are passthrough: the file keeps the stream's own bitrate, so the
            // quality acts as a ceiling. MP3 is re-encoded by LAME at the chosen bitrate and
            // always wants the best source.
            final boolean passthrough = wanted != DownloadsConfig.DownloadFormat.MP3;
            final int targetBitrate = quality.bitrateKbps * 1000;

            List<Format> candidates = response.getStreamingData()
                    .getAdaptiveFormatsList()
                    .stream()
                    .filter(format -> format.getMimeType().startsWith("audio/")
                            && AudioTagger.canTag(format.getMimeType())
                            && !format.getUrl().isBlank()
                            // A malformed entry reporting 0 would pass the ceiling below.
                            && format.getBitrate() > 0
                            && AudioTagger.isMp4(format.getMimeType()) != wantOpus)
                    .collect(Collectors.toList());

            Format best;
            if (passthrough) {
                // YouTube only offers a few rungs (opus ~48/70/160, AAC ~128/256). Nearest-match
                // would pick the same rung for every quality, so LOW must really cap the bitrate.
                best = candidates.stream()
                        .filter(format -> format.getBitrate() <= targetBitrate)
                        .max(Comparator.comparingInt(Format::getBitrate))
                        // Every rung is above the ceiling: fall back to the smallest one.
                        .orElseGet(() -> candidates.stream()
                                .min(Comparator.comparingInt(Format::getBitrate))
                                .orElse(null));
            } else {
                best = candidates.stream()
                        .max(Comparator.comparingInt(Format::getBitrate))
                        .orElse(null);
            }
            return best == null ? null : new ResolvedStream(best, response.getVideoDetails());
        } catch (Exception ex) {
            Logger.printException(() -> "Could not parse audio formats: " + videoId, ex);
            return null;
        }
    }
}
