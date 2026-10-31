package io.github.serenity_player.serenity.data.repository

import io.github.serenity_player.serenity.data.api.AlbumFullDto
import io.github.serenity_player.serenity.data.api.ArtistFullDto
import io.github.serenity_player.serenity.data.api.ImageDto
import io.github.serenity_player.serenity.data.api.PlaylistSummaryDto
import io.github.serenity_player.serenity.data.api.SearchPageDto
import io.github.serenity_player.serenity.data.api.SearchResponseDto
import io.github.serenity_player.serenity.data.api.SpotifyApi
import io.github.serenity_player.serenity.data.api.TrackDto
import io.github.serenity_player.serenity.library.AlbumSummary
import io.github.serenity_player.serenity.library.Artist
import io.github.serenity_player.serenity.library.Playlist
import io.github.serenity_player.serenity.library.Track
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

const val PAGE_SIZE = 50

/** Spotify caps search at 10 results per request for development-mode apps. */
const val SEARCH_PAGE_SIZE = 10

sealed interface PageResult<out T> {
    data class Success<T>(val items: List<T>, val hasNext: Boolean, val after: String? = null) : PageResult<T>
    data class Failure(val message: String) : PageResult<Nothing>
}

/** First page of every search type, fetched in a single request. */
data class SearchBundle(
    val tracks: PageResult.Success<Track>,
    val artists: PageResult.Success<Artist>,
    val albums: PageResult.Success<AlbumSummary>,
    val playlists: PageResult.Success<Playlist>,
)

sealed interface SearchResult {
    data class Success(val bundle: SearchBundle) : SearchResult
    data class Failure(val message: String) : SearchResult
}

private sealed interface Outcome<out R> {
    data class Ok<R>(val value: R) : Outcome<R>
    data class Fail(val message: String) : Outcome<Nothing>
}

private const val NOT_OWNED =
    "Spotify only shares track lists for playlists you own. You can still play this one with the buttons above."

class LibraryRepository(private val api: SpotifyApi) {

    // ---- Library ----

    suspend fun playlists(offset: Int): PageResult<Playlist> = load(
        call = { api.getPlaylists(PAGE_SIZE, offset) },
        map = { p -> PageResult.Success(p.items.mapNotNull { it.toDomain() }, hasNext = p.next != null) },
    )

    suspend fun savedTracks(offset: Int): PageResult<Track> = load(
        call = { api.getSavedTracks(PAGE_SIZE, offset) },
        map = { p -> PageResult.Success(p.items.mapNotNull { it.track?.toTrack(null) }, hasNext = p.next != null) },
    )

    suspend fun savedAlbums(offset: Int): PageResult<AlbumSummary> = load(
        call = { api.getSavedAlbums(PAGE_SIZE, offset) },
        map = { p -> PageResult.Success(p.items.mapNotNull { it.album?.toDomain() }, hasNext = p.next != null) },
    )

    suspend fun followedArtists(after: String?): PageResult<Artist> = load(
        call = { api.getFollowedArtists("artist", PAGE_SIZE, after) },
        map = { d ->
            val page = d.artists
            val next = page.cursors?.after
            PageResult.Success(page.items.mapNotNull { it.toDomain() }, hasNext = page.next != null && next != null, after = next)
        },
    )

    suspend fun playlistItems(id: String, offset: Int): PageResult<Track> = load(
        forbiddenMessage = NOT_OWNED,
        call = { api.getPlaylistItems(id, PAGE_SIZE, offset, "track") },
        map = { p ->
            PageResult.Success(
                // `item` since Feb 2026, `track` before.
                p.items.mapNotNull { (it.item ?: it.track)?.toTrack(null) },
                hasNext = p.next != null,
            )
        },
    )

    suspend fun albumTracks(id: String, offset: Int, albumArt: String?): PageResult<Track> = load(
        call = { api.getAlbumTracks(id, PAGE_SIZE, offset) },
        map = { p -> PageResult.Success(p.items.mapNotNull { it.toTrack(albumArt) }, hasNext = p.next != null) },
    )

    /** Albums and singles. (Artist top tracks were removed from the API in Feb 2026.) */
    suspend fun artistAlbums(id: String, offset: Int): PageResult<AlbumSummary> = load(
        call = { api.getArtistAlbums(id, "album,single", PAGE_SIZE, offset) },
        map = { p -> PageResult.Success(p.items.mapNotNull { it.toDomain() }, hasNext = p.next != null) },
    )

    // ---- Search ----

    suspend fun searchAll(query: String): SearchResult {
        val outcome = request(
            forbiddenMessage = null,
            call = { api.search(query, "track,artist,album,playlist", SEARCH_PAGE_SIZE, 0) },
            map = { b: SearchResponseDto ->
                SearchBundle(
                    tracks = PageResult.Success(b.tracks.mapped { it.toTrack(null) }, hasNext = b.tracks?.next != null),
                    artists = PageResult.Success(b.artists.mapped { it.toDomain() }, hasNext = b.artists?.next != null),
                    albums = PageResult.Success(b.albums.mapped { it.toDomain() }, hasNext = b.albums?.next != null),
                    playlists = PageResult.Success(b.playlists.mapped { it.toDomain() }, hasNext = b.playlists?.next != null),
                )
            },
        )
        return when (outcome) {
            is Outcome.Ok -> SearchResult.Success(outcome.value)
            is Outcome.Fail -> SearchResult.Failure(outcome.message)
        }
    }

    suspend fun searchTracks(query: String, offset: Int): PageResult<Track> = load(
        call = { api.search(query, "track", SEARCH_PAGE_SIZE, offset) },
        map = { b -> PageResult.Success(b.tracks.mapped { it.toTrack(null) }, hasNext = b.tracks?.next != null) },
    )

    suspend fun searchArtists(query: String, offset: Int): PageResult<Artist> = load(
        call = { api.search(query, "artist", SEARCH_PAGE_SIZE, offset) },
        map = { b -> PageResult.Success(b.artists.mapped { it.toDomain() }, hasNext = b.artists?.next != null) },
    )

    suspend fun searchAlbums(query: String, offset: Int): PageResult<AlbumSummary> = load(
        call = { api.search(query, "album", SEARCH_PAGE_SIZE, offset) },
        map = { b -> PageResult.Success(b.albums.mapped { it.toDomain() }, hasNext = b.albums?.next != null) },
    )

    suspend fun searchPlaylists(query: String, offset: Int): PageResult<Playlist> = load(
        call = { api.search(query, "playlist", SEARCH_PAGE_SIZE, offset) },
        map = { b -> PageResult.Success(b.playlists.mapped { it.toDomain() }, hasNext = b.playlists?.next != null) },
    )

    // ---- Plumbing ----

    private suspend fun <D, T> load(
        forbiddenMessage: String? = null,
        call: suspend () -> Response<D>,
        map: (D) -> PageResult<T>,
    ): PageResult<T> = when (val o = request(forbiddenMessage, call, map)) {
        is Outcome.Ok -> o.value
        is Outcome.Fail -> PageResult.Failure(o.message)
    }

    private suspend fun <D, R> request(
        forbiddenMessage: String?,
        call: suspend () -> Response<D>,
        map: (D) -> R,
    ): Outcome<R> {
        return try {
            val r = call()
            val body = r.body()
            when {
                r.isSuccessful && body != null -> Outcome.Ok(map(body))
                else -> {
                    val err = spotifyError(r)
                    if ((r.code() == 403 || r.code() == 404) && forbiddenMessage != null && !err.notRegistered) {
                        Outcome.Fail(forbiddenMessage)
                    } else {
                        Outcome.Fail(err.message)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.Fail("Can't reach Spotify.")
        }
    }
}

/** Search pages may contain null entries; drop them while mapping. */
private fun <T, R : Any> SearchPageDto<T>?.mapped(f: (T) -> R?): List<R> =
    this?.items.orEmpty().mapNotNull { item -> item?.let(f) }

/** Smallest image that is at least [minWidth] wide, else the first one. */
internal fun List<ImageDto>.best(minWidth: Int = 300): String? {
    val usable = filter { it.url.isNotBlank() }.sortedBy { it.width ?: Int.MAX_VALUE }
    return (usable.firstOrNull { (it.width ?: Int.MAX_VALUE) >= minWidth } ?: usable.firstOrNull())?.url
}

private fun PlaylistSummaryDto.toDomain(): Playlist? {
    if (id.isBlank()) return null
    return Playlist(
        id = id,
        uri = uri.ifBlank { "spotify:playlist:$id" },
        name = name,
        owner = owner?.displayName ?: owner?.id,
        artUrl = images.best(),
        trackCount = (items ?: tracks)?.total,
    )
}

private fun AlbumFullDto.toDomain(): AlbumSummary? {
    if (id.isBlank()) return null
    return AlbumSummary(
        id = id,
        uri = uri.ifBlank { "spotify:album:$id" },
        name = name,
        artists = artists.joinToString(", ") { it.name },
        artUrl = images.best(),
        year = releaseDate?.take(4),
        trackCount = totalTracks,
    )
}

private fun ArtistFullDto.toDomain(): Artist? {
    if (id.isBlank()) return null
    return Artist(id = id, uri = uri.ifBlank { "spotify:artist:$id" }, name = name, artUrl = images.best())
}

/** Local files can't be played remotely, so they're dropped. Handles episodes too (queue). */
internal fun TrackDto.toTrack(fallbackArt: String?): Track? {
    if (isLocal || uri.isBlank()) return null
    return Track(
        uri = uri,
        title = name,
        artists = artists.joinToString(", ") { it.name }.ifBlank { show?.name.orEmpty() },
        artUrl = album?.images?.best() ?: images.best() ?: fallbackArt,
        durationMs = durationMs,
    )
}
