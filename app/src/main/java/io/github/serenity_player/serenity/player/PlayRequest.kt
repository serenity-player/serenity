package io.github.serenity_player.serenity.player

/** What to start playing. */
sealed interface PlayRequest {
    /** A playlist, album or artist, optionally starting at a specific track. */
    data class Context(val uri: String, val offsetUri: String? = null) : PlayRequest

    /** An explicit list of track URIs, played in order (used for Liked Songs, which has no context). */
    data class Tracks(val uris: List<String>) : PlayRequest
}
