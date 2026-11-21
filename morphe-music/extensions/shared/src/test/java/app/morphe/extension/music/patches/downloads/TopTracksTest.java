/*
 * Copyright 2026 Morphe.
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.downloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the ranking used to decide what counts as a "most played" song.
 *
 * <p>The previous implementation compared a song's score to the <em>lowest</em> score
 * in the library. Because a decayed-to-zero entry sits in that library, the comparison
 * was {@code score >= 0}, which is always true: every play was treated as a top-N play
 * and the configured count had no effect. The rank below is what the fix relies on.
 */
public class TopTracksTest {

    private static List<Double> scores(Double... values) {
        return values.length == 0 ? Collections.emptyList() : Arrays.asList(values);
    }

    @Test
    public void firstPlayInEmptyLibraryIsRankZero() {
        assertEquals(0, TopTracks.rankOf(scores(), 1.0));
    }

    @Test
    public void rankCountsOnlyStrictlyHigherScores() {
        assertEquals(2, TopTracks.rankOf(scores(5.0, 3.0, 1.0), 2.0));
    }

    @Test
    public void tiesShareTheBetterRank() {
        assertEquals(1, TopTracks.rankOf(scores(5.0, 2.0, 2.0), 2.0));
    }

    @Test
    public void highestScoreRanksZero() {
        assertEquals(0, TopTracks.rankOf(scores(1.0, 2.0, 3.0), 10.0));
    }

    @Test
    public void lowestScoreIsRankedBehindEveryEntry() {
        assertEquals(3, TopTracks.rankOf(scores(1.0, 2.0, 3.0), 0.5));
    }

    /**
     * The regression: a freshly played song in a library of three heavily played
     * songs must not be considered top-1, which the old "compare to lowest" logic
     * got wrong as soon as a zero-scored entry existed.
     */
    @Test
    public void freshPlayIsNotTopOneInABusyLibrary() {
        List<Double> others = scores(10.0, 9.0, 0.0);
        int rank = TopTracks.rankOf(others, 1.0);
        assertEquals(2, rank);
        assertFalse("rank 2 is outside a top-1 selection", rank < 1);
        assertTrue("rank 2 is inside a top-3 selection", rank < 3);
    }

    /**
     * A decayed-to-zero neighbour does not outrank a fresh play, but it still means the
     * play is not the only entry: rank is 0 here, so a top-1 selection keeps it.
     */
    @Test
    public void decayedNeighbourDoesNotOutrankAFreshPlay() {
        assertEquals(0, TopTracks.rankOf(scores(0.0), 1.0));
    }

    /** A stale neighbour scoring above the new play pushes it down a slot. */
    @Test
    public void higherNeighbourOutranksAFreshPlay() {
        assertEquals(1, TopTracks.rankOf(scores(0.6), 0.4));
    }
}
