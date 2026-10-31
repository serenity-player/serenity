package io.github.serenity_player.serenity.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    private fun state(playing: Boolean, progress: Long, duration: Long) = PlaybackState(
        title = "Song",
        subtitle = "Artist",
        album = "Album",
        artUrl = null,
        isPlaying = playing,
        progressMs = progress,
        durationMs = duration,
        shuffle = false,
        repeat = RepeatMode.Off,
        deviceName = "Phone",
        volumePercent = 50,
        supportsVolume = true,
    )

    @Test
    fun positionAdvancesWhilePlaying() {
        assertEquals(13_000L, state(true, 10_000, 200_000).positionAt(syncedAt = 1_000, now = 4_000))
    }

    @Test
    fun positionHoldsWhilePaused() {
        assertEquals(10_000L, state(false, 10_000, 200_000).positionAt(syncedAt = 1_000, now = 99_000))
    }

    @Test
    fun positionNeverPassesTheEndOfTheTrack() {
        assertEquals(12_000L, state(true, 10_000, 12_000).positionAt(syncedAt = 0, now = 60_000))
    }

    @Test
    fun repeatCyclesOffContextTrack() {
        assertEquals(RepeatMode.Context, RepeatMode.Off.next())
        assertEquals(RepeatMode.Track, RepeatMode.Context.next())
        assertEquals(RepeatMode.Off, RepeatMode.Track.next())
    }

    @Test
    fun unknownRepeatStateMeansOff() {
        assertEquals(RepeatMode.Off, RepeatMode.from(null))
        assertEquals(RepeatMode.Track, RepeatMode.from("track"))
    }
}
