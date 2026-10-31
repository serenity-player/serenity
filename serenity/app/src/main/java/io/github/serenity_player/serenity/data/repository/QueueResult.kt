package io.github.serenity_player.serenity.data.repository

import io.github.serenity_player.serenity.library.Track

sealed interface QueueResult {
    data class Success(val current: Track?, val upcoming: List<Track>) : QueueResult
    data class Failure(val message: String) : QueueResult
}
