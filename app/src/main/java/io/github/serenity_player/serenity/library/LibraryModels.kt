package io.github.serenity_player.serenity.library

data class Track(val uri: String, val title: String, val artists: String, val artUrl: String?, val durationMs: Long)

data class Playlist(
    val id: String,
    val uri: String,
    val name: String,
    val owner: String?,
    val artUrl: String?,
    val trackCount: Int?,
)

data class AlbumSummary(
    val id: String,
    val uri: String,
    val name: String,
    val artists: String,
    val artUrl: String?,
    val year: String?,
    val trackCount: Int,
)

data class Artist(val id: String, val uri: String, val name: String, val artUrl: String?)
