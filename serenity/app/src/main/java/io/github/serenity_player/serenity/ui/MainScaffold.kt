package io.github.serenity_player.serenity.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.serenity_player.serenity.SerenityApp
import io.github.serenity_player.serenity.library.LibraryViewModel
import io.github.serenity_player.serenity.media.MediaPlaybackService
import io.github.serenity_player.serenity.search.SearchViewModel
import io.github.serenity_player.serenity.ui.library.LibraryScreen
import io.github.serenity_player.serenity.ui.nowplaying.DeviceSheet
import io.github.serenity_player.serenity.ui.nowplaying.MiniPlayer
import io.github.serenity_player.serenity.ui.nowplaying.NowPlayingScreen
import io.github.serenity_player.serenity.ui.nowplaying.QueueSheet
import io.github.serenity_player.serenity.ui.search.SearchScreen
import io.github.serenity_player.serenity.ui.theme.ArtTheme
import io.github.serenity_player.serenity.ui.theme.rememberArtColor

private const val TAB_NOW_PLAYING = 0
private const val TAB_LIBRARY = 1

/** At or above this width (tablets, unfolded foldables, landscape) the layout switches to a side rail. */
private const val WIDE_DP = 600

private data class TabSpec(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabSpec("Now Playing", Icons.Filled.MusicNote),
    TabSpec("Library", Icons.Filled.LibraryMusic),
    TabSpec("Search", Icons.Filled.Search),
)

/** Signed-in shell: navigation, mini player, banners, snackbars, device picker, queue and playback polling. */
@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val context = LocalContext.current
    // Process-wide instance: the notification service reads and controls the same player state.
    val np = (context.applicationContext as SerenityApp).nowPlaying
    val lib: LibraryViewModel = viewModel()
    val search: SearchViewModel = viewModel()
    val npUi by np.ui.collectAsStateWithLifecycle()
    val devices by np.devices.collectAsStateWithLifecycle()
    val queue by np.queue.collectAsStateWithLifecycle()
    val connectionIssue by np.connectionIssue.collectAsStateWithLifecycle()
    val premiumRequired by np.premiumRequired.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(TAB_NOW_PLAYING) }
    val wide = LocalConfiguration.current.screenWidthDp >= WIDE_DP

    // Poll only while the app is visible, whichever tab is open.
    LifecycleStartEffect(Unit) {
        np.startPolling()
        onStopOrDispose { np.stopPolling() }
    }
    LaunchedEffect(Unit) {
        np.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(msg.text, actionLabel = if (msg.offerDevices) "Devices" else null)
            if (result == SnackbarResult.ActionPerformed) np.openDevices()
        }
    }
    BackHandler(enabled = tab != TAB_NOW_PLAYING) { tab = TAB_NOW_PLAYING }

    // Android 13+ needs permission to show the media notification and lock-screen controls.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Keep the playback service (notification, lock screen, background updates) running while something plays.
    val playing = npUi.playback?.isPlaying == true
    LifecycleResumeEffect(playing) {
        if (playing) MediaPlaybackService.start(context)
        onPauseOrDispose { }
    }

    val pb = npUi.playback
    val seed = rememberArtColor(pb?.artUrl)

    ArtTheme(seed) {
        val colors = MaterialTheme.colorScheme
        val background: Brush =
            if (tab == TAB_NOW_PLAYING) Brush.verticalGradient(listOf(colors.primaryContainer, colors.background))
            else SolidColor(colors.background)

        Box(Modifier.fillMaxSize().background(background)) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = tab != TAB_NOW_PLAYING && pb != null,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                        ) {
                            if (pb != null) {
                                MiniPlayer(pb, onClick = { tab = TAB_NOW_PLAYING }, onPlayPause = np::playPause)
                            }
                        }
                        if (!wide) BottomNav(tab) { tab = it }
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding)) {
                    StatusBanners(connectionIssue, premiumRequired)
                    Row(Modifier.weight(1f).fillMaxSize()) {
                        if (wide) SideRail(tab) { tab = it }
                        AnimatedContent(
                            targetState = tab,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                            label = "tab",
                        ) { current ->
                            when (current) {
                                TAB_NOW_PLAYING -> NowPlayingScreen(
                                    vm = np,
                                    onDevices = np::openDevices,
                                    onQueue = np::openQueue,
                                    onAbout = { showAbout = true },
                                    onLogout = {
                                        MediaPlaybackService.stop(context)
                                        lib.reset()
                                        search.reset()
                                        onLogout()
                                    },
                                )
                                TAB_LIBRARY -> ReadableWidth { LibraryScreen(np = np, lib = lib) }
                                else -> ReadableWidth { SearchScreen(np = np, vm = search) }
                            }
                        }
                    }
                }
            }

            if (showAbout) AboutDialog(onDismiss = { showAbout = false })
            if (devices.visible) {
                DeviceSheet(
                    state = devices,
                    onDismiss = np::closeDevices,
                    onRefresh = np::refreshDevices,
                    onSelect = np::transferTo,
                )
            }
            if (queue.visible) {
                QueueSheet(state = queue, onDismiss = np::closeQueue)
            }
        }
    }
}

/** Long lists get uncomfortable to read when stretched across a tablet, so cap and centre them. */
@Composable
private fun ReadableWidth(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 840.dp).fillMaxHeight()) { content() }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        TABS.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun SideRail(selected: Int, onSelect: (Int) -> Unit) {
    NavigationRail(containerColor = Color.Transparent) {
        TABS.forEachIndexed { index, tab ->
            NavigationRailItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
