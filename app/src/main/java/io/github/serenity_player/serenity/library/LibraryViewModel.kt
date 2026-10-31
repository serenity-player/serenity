package io.github.serenity_player.serenity.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.serenity_player.serenity.SerenityApp

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SerenityApp).libraryRepository

    val playlists = Pager<Playlist>(viewModelScope) { offset, _ -> repo.playlists(offset) }
    val albums = Pager<AlbumSummary>(viewModelScope) { offset, _ -> repo.savedAlbums(offset) }
    val songs = Pager<Track>(viewModelScope) { offset, _ -> repo.savedTracks(offset) }
    val artists = Pager<Artist>(viewModelScope) { _, after -> repo.followedArtists(after) }

    val stack = PageStack(viewModelScope, repo)

    /** Called on logout so the next account doesn't see stale lists. */
    fun reset() {
        stack.clear()
        playlists.reset()
        albums.reset()
        songs.reset()
        artists.reset()
    }
}
