package io.github.serenity_player.serenity.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Everything is nullable or defaulted: Spotify omits fields for ads, episodes and idle devices.

@Serializable
data class PlaybackStateDto(
    val device: DeviceDto? = null,
    @SerialName("repeat_state") val repeatState: String? = null,
    @SerialName("shuffle_state") val shuffleState: Boolean = false,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long? = null,
    @SerialName("currently_playing_type") val type: String? = null,
    val item: ItemDto? = null,
)

@Serializable
data class DeviceDto(
    val id: String? = null,
    val name: String = "",
    val type: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("is_restricted") val isRestricted: Boolean = false,
    @SerialName("volume_percent") val volumePercent: Int? = null,
    @SerialName("supports_volume") val supportsVolume: Boolean = false,
)

/** Covers both tracks and podcast episodes. */
@Serializable
data class ItemDto(
    val id: String? = null,
    val name: String = "",
    @SerialName("duration_ms") val durationMs: Long = 0,
    val artists: List<ArtistDto> = emptyList(),
    val album: AlbumDto? = null,
    val show: ShowDto? = null,
    val images: List<ImageDto> = emptyList(),
)

@Serializable data class ArtistDto(val id: String? = null, val name: String = "")
@Serializable data class AlbumDto(val name: String = "", val images: List<ImageDto> = emptyList())
@Serializable data class ShowDto(val name: String = "")
@Serializable data class ImageDto(val url: String = "", val width: Int? = null, val height: Int? = null)

@Serializable
data class DevicesDto(val devices: List<DeviceDto> = emptyList())

@Serializable
data class TransferBody(
    @SerialName("device_ids") val deviceIds: List<String>,
    val play: Boolean,
)

// ---- Library ----

@Serializable
data class PagedDto<T>(val items: List<T> = emptyList(), val next: String? = null, val total: Int? = null)

@Serializable data class OwnerDto(val id: String? = null, @SerialName("display_name") val displayName: String? = null)
@Serializable data class CountDto(val total: Int? = null)

/** Playlist summary. The track count lives in `items` since Feb 2026 (was `tracks`); accept both. */
@Serializable
data class PlaylistSummaryDto(
    val id: String = "",
    val uri: String = "",
    val name: String = "",
    val images: List<ImageDto> = emptyList(),
    val owner: OwnerDto? = null,
    val items: CountDto? = null,
    val tracks: CountDto? = null,
)

@Serializable
data class TrackDto(
    val id: String? = null,
    val uri: String = "",
    val name: String = "",
    @SerialName("duration_ms") val durationMs: Long = 0,
    val artists: List<ArtistDto> = emptyList(),
    val album: AlbumDto? = null,
    @SerialName("is_local") val isLocal: Boolean = false,
    val images: List<ImageDto> = emptyList(), // episodes carry their art here
    val show: ShowDto? = null,
)

@Serializable data class SavedTrackDto(val track: TrackDto? = null)

@Serializable
data class AlbumFullDto(
    val id: String = "",
    val uri: String = "",
    val name: String = "",
    val artists: List<ArtistDto> = emptyList(),
    val images: List<ImageDto> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("total_tracks") val totalTracks: Int = 0,
)

@Serializable data class SavedAlbumDto(val album: AlbumFullDto? = null)

/** One playlist entry. The track sits under `item` since Feb 2026 (was `track`); accept both. */
@Serializable data class PlaylistEntryDto(val item: TrackDto? = null, val track: TrackDto? = null)

@Serializable data class CursorsDto(val after: String? = null)

@Serializable
data class CursorPagedDto<T>(
    val items: List<T> = emptyList(),
    val cursors: CursorsDto? = null,
    val next: String? = null,
)

@Serializable
data class ArtistFullDto(
    val id: String = "",
    val uri: String = "",
    val name: String = "",
    val images: List<ImageDto> = emptyList(),
)

@Serializable data class FollowedArtistsDto(val artists: CursorPagedDto<ArtistFullDto> = CursorPagedDto())

// ---- Play with context ----

@Serializable data class OffsetDto(val uri: String? = null, val position: Int? = null)

@Serializable
data class PlayBody(
    @SerialName("context_uri") val contextUri: String? = null,
    val uris: List<String>? = null,
    val offset: OffsetDto? = null,
)

// ---- Search & queue ----

/** Search pages can contain null entries (notably playlists), so elements are nullable. */
@Serializable
data class SearchPageDto<T>(val items: List<T?> = emptyList(), val next: String? = null)

@Serializable
data class SearchResponseDto(
    val tracks: SearchPageDto<TrackDto>? = null,
    val artists: SearchPageDto<ArtistFullDto>? = null,
    val albums: SearchPageDto<AlbumFullDto>? = null,
    val playlists: SearchPageDto<PlaylistSummaryDto>? = null,
)

@Serializable
data class QueueDto(
    @SerialName("currently_playing") val currentlyPlaying: TrackDto? = null,
    val queue: List<TrackDto> = emptyList(),
)
