package io.github.serenity_player.serenity.ui.nowplaying

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.serenity_player.serenity.player.NowPlayingViewModel
import io.github.serenity_player.serenity.player.PlaybackState
import io.github.serenity_player.serenity.player.RepeatMode
import kotlinx.coroutines.delay

private const val WIDE_DP = 600

@Composable
fun NowPlayingScreen(
    vm: NowPlayingViewModel,
    onDevices: () -> Unit,
    onQueue: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val wide = LocalConfiguration.current.screenWidthDp >= WIDE_DP
    Column(modifier.fillMaxSize().padding(horizontal = if (wide) 40.dp else 24.dp)) {
        TopRow(
            deviceName = ui.playback?.deviceName,
            onDevices = onDevices,
            onQueue = onQueue,
            onAbout = onAbout,
            onLogout = { vm.reset(); onLogout() },
        )
        val pb = ui.playback
        when {
            ui.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator()
            }
            pb == null -> EmptyState(onChooseDevice = onDevices, modifier = Modifier.weight(1f))
            else -> PlayerContent(pb, ui.syncedAt, vm, wide, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TopRow(
    deviceName: String?,
    onDevices: () -> Unit,
    onQueue: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onDevices, modifier = Modifier.weight(1f, fill = false)) {
            Icon(Icons.Filled.Devices, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                deviceName ?: "Choose a device",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQueue) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("About") }, onClick = { menu = false; onAbout() })
                    DropdownMenuItem(text = { Text("Log out") }, onClick = { menu = false; onLogout() })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onChooseDevice: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Nothing playing", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Start playing something in Spotify on any of your devices and it will show up here.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onChooseDevice) {
            Icon(Icons.Filled.Devices, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose a device")
        }
    }
}

/** Phones: one scrolling column. Tablets, unfolded foldables and landscape: art on the left, controls on the right. */
@Composable
private fun PlayerContent(pb: PlaybackState, syncedAt: Long, vm: NowPlayingViewModel, wide: Boolean, modifier: Modifier) {
    // The cover eases down a little when paused, which reads as "stopped" without any extra UI.
    val artScale by animateFloatAsState(
        targetValue = if (pb.isPlaying) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "artScale",
    )
    val scaled = Modifier.graphicsLayer {
        scaleX = artScale
        scaleY = artScale
    }

    if (wide) {
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                AlbumArt(
                    pb.artUrl,
                    pb.album,
                    Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).then(scaled),
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    TrackDetails(pb, syncedAt, vm)
                }
            }
        }
    } else {
        Column(
            modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AlbumArt(
                pb.artUrl,
                pb.album,
                Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f).then(scaled),
            )
            TrackDetails(pb, syncedAt, vm)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrackDetails(pb: PlaybackState, syncedAt: Long, vm: NowPlayingViewModel) {
    AnimatedContent(
        targetState = pb.title to pb.subtitle,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
        label = "trackText",
    ) { (title, subtitle) ->
        Column(Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    SeekBar(pb, syncedAt, onSeek = vm::seek)
    Controls(pb, vm)
    if (pb.supportsVolume && pb.volumePercent != null) {
        VolumeRow(pb.volumePercent, onChange = vm::setVolume)
    }
}

@Composable
private fun AlbumArt(url: String?, album: String?, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(16.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = album?.let { "Album art for $it" } ?: "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(72.dp))
        }
    }
}

@Composable
private fun SeekBar(pb: PlaybackState, syncedAt: Long, onSeek: (Long) -> Unit) {
    // Tick locally between polls so the bar moves smoothly.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(pb.isPlaying, syncedAt) {
        now = SystemClock.elapsedRealtime()
        while (pb.isPlaying) {
            delay(250)
            now = SystemClock.elapsedRealtime()
        }
    }
    var drag by remember { mutableStateOf<Float?>(null) }
    val duration = pb.durationMs.coerceAtLeast(1L).toFloat()
    val position = (drag ?: pb.positionAt(syncedAt, now).toFloat()).coerceIn(0f, duration)

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = position,
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { onSeek(it.toLong()) }
                drag = null
            },
            valueRange = 0f..duration,
            enabled = pb.durationMs > 0,
            // Spoken instead of a bare percentage: "1:05 of 3:42".
            modifier = Modifier.semantics {
                stateDescription = "${formatTime(position.toLong())} of ${formatTime(duration.toLong())}"
            },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position.toLong()), style = MaterialTheme.typography.labelMedium)
            Text("-" + formatTime((duration - position).toLong()), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Controls(pb: PlaybackState, vm: NowPlayingViewModel) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::toggleShuffle) {
            Icon(Icons.Filled.Shuffle, contentDescription = if (pb.shuffle) "Shuffle on" else "Shuffle off", tint = if (pb.shuffle) active else inactive)
        }
        IconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = vm::playPause, modifier = Modifier.size(72.dp)) {
            Crossfade(targetState = pb.isPlaying, animationSpec = tween(150), label = "playPause") { playing ->
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        IconButton(onClick = vm::next, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = vm::cycleRepeat) {
            Icon(
                if (pb.repeat == RepeatMode.Track) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat: ${pb.repeat.name.lowercase()}",
                tint = if (pb.repeat == RepeatMode.Off) inactive else active,
            )
        }
    }
}

@Composable
private fun VolumeRow(volume: Int, onChange: (Int) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val value = drag ?: volume.toFloat()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { onChange(it.toInt()) }
                drag = null
            },
            valueRange = 0f..100f,
            modifier = Modifier
                .weight(1f)
                .semantics { stateDescription = "Volume ${value.toInt()} percent" },
        )
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
