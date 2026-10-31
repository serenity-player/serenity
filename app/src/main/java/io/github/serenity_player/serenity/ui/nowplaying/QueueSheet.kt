package io.github.serenity_player.serenity.ui.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.serenity_player.serenity.player.QueueUi
import io.github.serenity_player.serenity.ui.library.TrackRow

/** The playing track plus what's coming up. Spotify's API can't reorder or jump, so rows are display-only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: QueueUi, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            when {
                state.loading && state.current == null && state.upcoming.isEmpty() ->
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }

                state.current == null && state.upcoming.isEmpty() ->
                    Text(
                        "Nothing is queued. Use the ⋮ menu on any song to add it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                    )

                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    state.current?.let { current ->
                        item { SectionLabel("Now playing") }
                        item { TrackRow(current) }
                    }
                    if (state.upcoming.isNotEmpty()) {
                        item { SectionLabel("Next up") }
                        itemsIndexed(state.upcoming) { _, track -> TrackRow(track) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
    )
}
