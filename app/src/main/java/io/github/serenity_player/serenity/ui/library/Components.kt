package io.github.serenity_player.serenity.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.serenity_player.serenity.library.Pager
import io.github.serenity_player.serenity.library.Track

/** Cover art. Coil caches images in memory and on disk, so scrolling back is instant. */
@Composable
fun Art(url: String?, modifier: Modifier = Modifier, circle: Boolean = false, corner: Dp = 8.dp) {
    val shape = if (circle) CircleShape else RoundedCornerShape(corner)
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    subtitle: String?,
    artUrl: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (!subtitle.isNullOrBlank()) {
            { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        leadingContent = { Art(artUrl, Modifier.size(56.dp), circle = circle) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    )
}

/** A song row. With [onAddToQueue] it gets an overflow menu; with no [onClick] it's display-only (queue view). */
@Composable
fun TrackRow(track: Track, onAddToQueue: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    MediaRow(
        title = track.title,
        subtitle = track.artists,
        artUrl = track.artUrl,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(track.durationMs), style = MaterialTheme.typography.labelMedium)
                if (onAddToQueue != null) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { open = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${track.title}")
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            DropdownMenuItem(text = { Text("Add to queue") }, onClick = { open = false; onAddToQueue() })
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun ListFooter(loading: Boolean, error: String?, showEmpty: Boolean, emptyText: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            showEmpty -> Text(
                emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Calls [onLoadMore] whenever the list is within [buffer] items of its end (including when it's short). */
@Composable
fun LazyListState.OnNearEnd(itemCount: Int, buffer: Int = 6, onLoadMore: () -> Unit) {
    val state = this
    val nearEnd by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 1 - buffer
        }
    }
    LaunchedEffect(nearEnd, itemCount) { if (nearEnd) onLoadMore() }
}

/** A lazily paged list with pull-to-refresh and a loading/error/empty footer. [row] also gets the list so far. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PagedList(
    pager: Pager<T>,
    emptyText: String,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    row: @Composable (index: Int, item: T, all: List<T>) -> Unit,
) {
    val state by pager.state.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    // The spinner belongs to a user-initiated refresh, not to ordinary "load more" paging.
    LaunchedEffect(state.loading) { if (!state.loading) refreshing = false }
    listState.OnNearEnd(itemCount = state.items.size) { pager.loadNext() }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            pager.refresh()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (header != null) item { header() }
            // No keys: playlists can contain the same track twice.
            itemsIndexed(state.items) { index, item -> row(index, item, state.items) }
            item {
                ListFooter(
                    loading = state.loading && !refreshing,
                    error = state.error,
                    showEmpty = state.initialized && state.endReached && state.items.isEmpty() && state.error == null,
                    emptyText = emptyText,
                    onRetry = pager::loadNext,
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
