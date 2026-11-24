package app.morphe.extension.music.patches.downloads;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * MediaSession republishes metadata several times per song. Scoring every update multiplies one
 * play; ignoring every repeat makes a looped track uncountable. These tests pin the boundary.
 */
public class PlayScoringTest {

    private static final long WINDOW = 15_000;

    @Test
    public void firstPlayIsScored() {
        assertTrue(LocalDownloadManager.shouldScorePlay(null, 0L, "abc", 1_000L));
    }

    @Test
    public void nullVideoIdIsNeverScored() {
        assertFalse(LocalDownloadManager.shouldScorePlay("abc", 0L, null, 1_000L));
    }

    @Test
    public void immediateRepublicationOfSameSongIsIgnored() {
        assertFalse(LocalDownloadManager.shouldScorePlay("abc", 1_000L, "abc", 1_800L));
    }

    @Test
    public void republicationAtTheEdgeOfTheWindowIsStillIgnored() {
        assertFalse(LocalDownloadManager.shouldScorePlay("abc", 1_000L, "abc", 1_000L + WINDOW - 1));
    }

    @Test
    public void replayAfterTheWindowCounts() {
        assertTrue(LocalDownloadManager.shouldScorePlay("abc", 1_000L, "abc", 1_000L + WINDOW));
    }

    @Test
    public void loopingOneTrackScoresEveryPass() {
        // A three minute track on repeat: each restart is far outside the window.
        assertTrue(LocalDownloadManager.shouldScorePlay("abc", 1_000L, "abc", 181_000L));
    }

    @Test
    public void differentSongIsAlwaysScored() {
        assertTrue(LocalDownloadManager.shouldScorePlay("abc", 1_000L, "xyz", 1_100L));
    }

    @Test
    public void returningToAPreviousSongIsScored() {
        // A -> B -> A: the last scored id is B, so A counts again immediately.
        assertTrue(LocalDownloadManager.shouldScorePlay("bbb", 1_000L, "aaa", 1_100L));
    }

    @Test
    public void clockGoingBackwardsStillScoresTheNewSong() {
        // A wall-clock jump backwards must not make a different song look like a repeat.
        assertTrue(LocalDownloadManager.shouldScorePlay("abc", 500_000L, "xyz", 1_000L));
    }

    @Test
    public void clockGoingBackwardsStillScoresTheSameSong() {
        // The dangerous case: the same id, with time appearing to run backwards. Ignoring it
        // would block every replay of this track until the clock caught up.
        assertTrue(LocalDownloadManager.shouldScorePlay("abc", 500_000L, "abc", 1_000L));
    }
}
