package io.github.serenity_player.serenity.library

import io.github.serenity_player.serenity.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DetailKind { Playlist, Album }

/** A page pushed on top of the Library or Search tab. */
sealed interface Page {
    fun release()
}

/** A playlist or album. The header comes from the list row, so it shows instantly. */
data class DetailUi(
    val kind: DetailKind,
    val uri: String,
    val title: String,
    val subtitle: String?,
    val artUrl: String?,
    val pager: Pager<Track>,
) : Page {
    override fun release() = pager.reset()
}

/** An artist and their albums and singles. */
data class ArtistUi(val artist: Artist, val pager: Pager<AlbumSummary>) : Page {
    override fun release() = pager.reset()
}

/** Back stack for one tab, so Search -> Artist -> Album -> back -> back works. */
class PageStack(private val scope: CoroutineScope, private val repo: LibraryRepository) {
    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages.asStateFlow()

    fun openPlaylist(p: Playlist) {
        val pager = Pager<Track>(scope) { offset, _ -> repo.playlistItems(p.id, offset) }
        _pages.update {
            it + DetailUi(DetailKind.Playlist, p.uri, p.name, p.owner?.let { o -> "By $o" }, p.artUrl, pager)
        }
        pager.loadNext()
    }

    fun openAlbum(a: AlbumSummary) {
        val pager = Pager<Track>(scope) { offset, _ -> repo.albumTracks(a.id, offset, a.artUrl) }
        val subtitle = listOfNotNull(a.artists.takeIf { it.isNotBlank() }, a.year).joinToString(" · ")
        _pages.update { it + DetailUi(DetailKind.Album, a.uri, a.name, subtitle, a.artUrl, pager) }
        pager.loadNext()
    }

    fun openArtist(artist: Artist) {
        val pager = Pager<AlbumSummary>(scope) { offset, _ -> repo.artistAlbums(artist.id, offset) }
        _pages.update { it + ArtistUi(artist, pager) }
        pager.loadNext()
    }

    fun back() {
        val top = _pages.value.lastOrNull() ?: return
        top.release()
        _pages.update { it.dropLast(1) }
    }

    fun clear() {
        _pages.value.forEach { it.release() }
        _pages.value = emptyList()
    }
}
