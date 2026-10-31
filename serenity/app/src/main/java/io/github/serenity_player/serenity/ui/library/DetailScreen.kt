package io.github.serenity_player.serenity.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.serenity_player.serenity.library.DetailUi
import io.github.serenity_player.serenity.player.NowPlayingViewModel
import io.github.serenity_player.serenity.player.PlayRequest

/** Playlist or album page: Play and Shuffle for the whole thing, or tap a track to play from there. */
@Composable
fun DetailScreen(d: DetailUi, np: NowPlayingViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    PagedList(
        pager = d.pager,
        emptyText = "No tracks to show.",
        modifier = modifier,
        header = {
            PageHeader(
                title = d.title,
                subtitle = d.subtitle,
                artUrl = d.artUrl,
                circle = false,
                onBack = onBack,
                onPlay = { np.play(PlayRequest.Context(d.uri)) },
                onShuffle = { np.play(PlayRequest.Context(d.uri), shuffle = true) },
            )
        },
    ) { _, track, _ ->
        TrackRow(track, onAddToQueue = { np.addToQueue(track) }) {
            np.play(PlayRequest.Context(d.uri, offsetUri = track.uri))
        }
    }
}

/** Shared header for playlist, album and artist pages. */
@Composable
fun PageHeader(
    title: String,
    subtitle: String?,
    artUrl: String?,
    circle: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Art(artUrl, Modifier.size(if (circle) 160.dp else 200.dp), circle = circle, corner = 20.dp)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }
            OutlinedButton(onClick = onShuffle) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Shuffle")
            }
        }
    }
}
