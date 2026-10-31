package io.github.serenity_player.serenity.player

enum class RepeatMode(val api: String) {
    Off("off"), Context("context"), Track("track");

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun from(api: String?): RepeatMode = entries.firstOrNull { it.api == api } ?: Off
    }
}

data class PlaybackState(
    val title: String,
    val subtitle: String,
    val album: String?,
    val artUrl: String?,
    val isPlaying: Boolean,
    val progressMs: Long,
    val durationMs: Long,
    val shuffle: Boolean,
    val repeat: RepeatMode,
    val deviceName: String?,
    val volumePercent: Int?,
    val supportsVolume: Boolean,
) {
    /** Estimated position now, given when [progressMs] was measured (SystemClock.elapsedRealtime). */
    fun positionAt(syncedAt: Long, now: Long): Long =
        (progressMs + if (isPlaying) now - syncedAt else 0L).coerceIn(0L, durationMs.coerceAtLeast(0L))
}

data class NowPlayingUi(
    val loading: Boolean = true,
    val playback: PlaybackState? = null, // null = no active device / nothing playing
    val syncedAt: Long = 0L,
)
