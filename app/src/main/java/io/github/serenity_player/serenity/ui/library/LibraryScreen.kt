package io.github.serenity_player.serenity.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.serenity_player.serenity.library.LibraryViewModel
import io.github.serenity_player.serenity.library.Playlist
import io.github.serenity_player.serenity.player.NowPlayingViewModel
import io.github.serenity_player.serenity.player.PlayRequest

private val TITLES = listOf("Playlists", "Albums", "Songs", "Artists")

@Composable
fun LibraryScreen(np: NowPlayingViewModel, lib: LibraryViewModel, modifier: Modifier = Modifier) {
    val pages by lib.stack.pages.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) } // hoisted so it survives opening a page
    // Hoisted list states keep each tab's scroll position while a page is open on top.
    val listStates = List(TITLES.size) { rememberLazyListState() }

    PageContainer(pages, lib.stack, np, modifier) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .semantics { heading() },
            )
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                TITLES.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> PagedList(lib.playlists, "No playlists yet.", Modifier.weight(1f), listState = listStates[0]) { _, p, _ ->
                    MediaRow(p.name, playlistSubtitle(p), p.artUrl, onClick = { lib.stack.openPlaylist(p) })
                }
                1 -> PagedList(lib.albums, "No saved albums yet.", Modifier.weight(1f), listState = listStates[1]) { _, a, _ ->
                    MediaRow(
                        a.name,
                        listOfNotNull(a.artists.takeIf { it.isNotBlank() }, a.year).joinToString(" · "),
                        a.artUrl,
                        onClick = { lib.stack.openAlbum(a) },
                    )
                }
                2 -> PagedList(lib.songs, "No liked songs yet.", Modifier.weight(1f), listState = listStates[2]) { index, track, all ->
                    // Liked Songs has no playable context, so queue this track and the ones after it.
                    TrackRow(track, onAddToQueue = { np.addToQueue(track) }) {
                        np.play(PlayRequest.Tracks(all.drop(index).take(100).map { it.uri }))
                    }
                }
                else -> PagedList(lib.artists, "You aren't following any artists yet.", Modifier.weight(1f), listState = listStates[3]) { _, a, _ ->
                    MediaRow(a.name, null, a.artUrl, circle = true, onClick = { lib.stack.openArtist(a) })
                }
            }
        }
    }
}

private fun playlistSubtitle(p: Playlist): String = listOfNotNull(
    p.trackCount?.let { "$it songs" },
    p.owner,
).joinToString(" · ")
