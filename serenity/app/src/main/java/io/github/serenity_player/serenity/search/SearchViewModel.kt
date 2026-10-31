package io.github.serenity_player.serenity.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.serenity_player.serenity.SerenityApp
import io.github.serenity_player.serenity.data.repository.SEARCH_PAGE_SIZE
import io.github.serenity_player.serenity.data.repository.SearchResult
import io.github.serenity_player.serenity.library.AlbumSummary
import io.github.serenity_player.serenity.library.Artist
import io.github.serenity_player.serenity.library.PageStack
import io.github.serenity_player.serenity.library.Pager
import io.github.serenity_player.serenity.library.Playlist
import io.github.serenity_player.serenity.library.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUi(
    val query: String = "",
    val loading: Boolean = false,
    val searched: Boolean = false,
    val error: String? = null,
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SerenityApp).libraryRepository

    private val _ui = MutableStateFlow(SearchUi())
    val ui: StateFlow<SearchUi> = _ui.asStateFlow()

    /** The query the current results belong to; "Show more" pages use it. */
    private var committedQuery = ""
    private var job: Job? = null

    // One request fills all four sections; each section then pages on its own ("Show more").
    val tracks = Pager<Track>(viewModelScope, SEARCH_PAGE_SIZE) { offset, _ -> repo.searchTracks(committedQuery, offset) }
    val artists = Pager<Artist>(viewModelScope, SEARCH_PAGE_SIZE) { offset, _ -> repo.searchArtists(committedQuery, offset) }
    val albums = Pager<AlbumSummary>(viewModelScope, SEARCH_PAGE_SIZE) { offset, _ -> repo.searchAlbums(committedQuery, offset) }
    val playlists = Pager<Playlist>(viewModelScope, SEARCH_PAGE_SIZE) { offset, _ -> repo.searchPlaylists(committedQuery, offset) }

    val stack = PageStack(viewModelScope, repo)

    fun onQueryChange(query: String) {
        job?.cancel()
        if (query.isBlank()) {
            clearResults()
            _ui.value = SearchUi(query = query)
            return
        }
        _ui.update { it.copy(query = query) }
        // Debounce so typing doesn't spend Spotify's rate limit on every keystroke.
        job = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query.trim())
        }
    }

    /** Search now (keyboard action, or retry after an error). */
    fun submit() {
        job?.cancel()
        val q = _ui.value.query.trim()
        if (q.isEmpty()) return
        job = viewModelScope.launch { runSearch(q) }
    }

    private suspend fun runSearch(query: String) {
        _ui.update { it.copy(loading = true, error = null) }
        when (val r = repo.searchAll(query)) {
            is SearchResult.Success -> {
                committedQuery = query
                tracks.seed(r.bundle.tracks)
                artists.seed(r.bundle.artists)
                albums.seed(r.bundle.albums)
                playlists.seed(r.bundle.playlists)
                _ui.update { it.copy(loading = false, searched = true) }
            }
            is SearchResult.Failure -> _ui.update { it.copy(loading = false, error = r.message) }
        }
    }

    private fun clearResults() {
        committedQuery = ""
        tracks.reset(); artists.reset(); albums.reset(); playlists.reset()
    }

    /** Called on logout. */
    fun reset() {
        job?.cancel()
        stack.clear()
        clearResults()
        _ui.value = SearchUi()
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
