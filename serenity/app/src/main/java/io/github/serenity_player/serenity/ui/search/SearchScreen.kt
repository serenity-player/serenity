package io.github.serenity_player.serenity.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.serenity_player.serenity.library.ListState
import io.github.serenity_player.serenity.player.NowPlayingViewModel
import io.github.serenity_player.serenity.player.PlayRequest
import io.github.serenity_player.serenity.search.SearchViewModel
import io.github.serenity_player.serenity.ui.library.ListFooter
import io.github.serenity_player.serenity.ui.library.MediaRow
import io.github.serenity_player.serenity.ui.library.PageContainer
import io.github.serenity_player.serenity.ui.library.TrackRow

@Composable
fun SearchScreen(np: NowPlayingViewModel, vm: SearchViewModel, modifier: Modifier = Modifier) {
    val pages by vm.stack.pages.collectAsStateWithLifecycle()
    PageContainer(pages, vm.stack, np, modifier) { SearchContent(np, vm, Modifier) }
}

@Composable
private fun SearchContent(np: NowPlayingViewModel, vm: SearchViewModel, modifier: Modifier) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val tracks by vm.tracks.state.collectAsStateWithLifecycle()
    val artists by vm.artists.state.collectAsStateWithLifecycle()
    val albums by vm.albums.state.collectAsStateWithLifecycle()
    val playlists by vm.playlists.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::onQueryChange,
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Songs, artists, albums, playlists") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (ui.query.isNotEmpty()) {
                {
                    IconButton(onClick = { vm.onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit(); focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        val nothingFound = ui.searched && !ui.loading && ui.error == null &&
            tracks.items.isEmpty() && artists.items.isEmpty() && albums.items.isEmpty() && playlists.items.isEmpty()

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (ui.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) }
            }
            ui.error?.let { message ->
                item { ListFooter(loading = false, error = message, showEmpty = false, emptyText = "", onRetry = vm::submit) }
            }
            if (nothingFound) {
                item {
                    Text(
                        "No results for “${ui.query.trim()}”",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }

            section("Songs", tracks, onMore = vm.tracks::loadNext) { t ->
                // Tapping plays just this song; the overflow menu adds it to the queue.
                TrackRow(t, onAddToQueue = { np.addToQueue(t) }) { np.play(PlayRequest.Tracks(listOf(t.uri))) }
            }
            section("Artists", artists, onMore = vm.artists::loadNext) { a ->
                MediaRow(a.name, null, a.artUrl, circle = true, onClick = { vm.stack.openArtist(a) })
            }
            section("Albums", albums, onMore = vm.albums::loadNext) { a ->
                MediaRow(
                    a.name,
                    listOfNotNull(a.artists.takeIf { it.isNotBlank() }, a.year).joinToString(" · "),
                    a.artUrl,
                    onClick = { vm.stack.openAlbum(a) },
                )
            }
            section("Playlists", playlists, onMore = vm.playlists::loadNext) { p ->
                MediaRow(
                    p.name,
                    listOfNotNull(p.trackCount?.let { "$it songs" }, p.owner).joinToString(" · "),
                    p.artUrl,
                    onClick = { vm.stack.openPlaylist(p) },
                )
            }
        }
    }
}

/** A titled group of results with a "Show more" button while Spotify has more. */
private fun <T> LazyListScope.section(
    title: String,
    state: ListState<T>,
    onMore: () -> Unit,
    row: @Composable (T) -> Unit,
) {
    if (state.items.isEmpty()) return
    item {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
    // No keys: results can repeat.
    items(state.items.size) { index -> row(state.items[index]) }
    if (!state.endReached || state.loading) {
        item {
            if (state.error != null) {
                ListFooter(loading = false, error = state.error, showEmpty = false, emptyText = "", onRetry = onMore)
            } else {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), Alignment.Center) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = onMore) { Text("Show more") }
                }
            }
        }
    }
}
