package io.github.serenity_player.serenity.player

import io.github.serenity_player.serenity.library.Track

data class QueueUi(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val current: Track? = null,
    val upcoming: List<Track> = emptyList(),
    val error: String? = null,
)
