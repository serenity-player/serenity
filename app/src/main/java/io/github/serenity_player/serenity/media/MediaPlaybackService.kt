package io.github.serenity_player.serenity.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as SessionPlaybackState
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.github.serenity_player.serenity.MainActivity
import io.github.serenity_player.serenity.R
import io.github.serenity_player.serenity.SerenityApp
import io.github.serenity_player.serenity.player.NowPlayingUi
import io.github.serenity_player.serenity.player.NowPlayingViewModel
import io.github.serenity_player.serenity.player.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mirrors Spotify's remote playback into a MediaSession, a media notification (lock screen, quick settings,
 * Bluetooth buttons) and the home-screen widget. It plays no audio: every control goes to the Web API.
 *
 * It runs as a foreground service while something is playing, keeps state fresh in the background, and
 * stops itself after a while with nothing to control.
 */
class MediaPlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var np: NowPlayingViewModel
    private lateinit var session: MediaSession
    private lateinit var manager: NotificationManager

    private var latest = NowPlayingUi()
    private var art: Bitmap? = null
    private var artUrl: String? = null
    private var postedKey: List<Any?>? = null
    private var metadataKey: List<Any?>? = null
    private var idleJob: Job? = null

    private val callback = object : MediaSession.Callback() {
        override fun onPlay() { if (latest.playback?.isPlaying == false) np.playPause() }
        override fun onPause() { if (latest.playback?.isPlaying == true) np.playPause() }
        override fun onSkipToNext() = np.next()
        override fun onSkipToPrevious() = np.previous()
        override fun onSeekTo(pos: Long) = np.seek(pos)
        override fun onStop() = stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        np = (application as SerenityApp).nowPlaying
        manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Playback controls", NotificationManager.IMPORTANCE_LOW)
        )
        session = MediaSession(this, "Serenity").apply {
            setCallback(callback)
            setSessionActivity(openAppIntent())
            isActive = true
        }
        // Must happen right after startForegroundService().
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

        np.startPolling(POLL_HOLDER)
        scope.launch { np.ui.collect { onState(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (action != null) {
            scope.launch {
                // After a process restart there's no state yet; give the first poll a moment.
                withTimeoutOrNull(READY_TIMEOUT_MS) { np.ui.first { !it.loading } }
                when (action) {
                    ACTION_TOGGLE -> np.playPause()
                    ACTION_NEXT -> np.next()
                    ACTION_PREVIOUS -> np.previous()
                }
            }
        }
        return START_NOT_STICKY // never resurrect in the background; the app starts us when it's visible
    }

    override fun onDestroy() {
        idleJob?.cancel()
        scope.cancel()
        np.stopPolling(POLL_HOLDER)
        session.isActive = false
        session.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        manager.cancel(NOTIFICATION_ID)
        WidgetUpdater.update(this, null, null)
        super.onDestroy()
    }

    // ---- State -> session / notification / widget ----

    private fun onState(ui: NowPlayingUi) {
        latest = ui
        val pb = ui.playback
        if (pb?.artUrl != artUrl) {
            artUrl = pb?.artUrl
            art = null
            artUrl?.let { url -> scope.launch { loadArt(url) } }
        }
        updateSession(ui)
        refreshNotification()
        if (!ui.loading) updateIdleTimer(pb)
    }

    private suspend fun loadArt(url: String) {
        val request = ImageRequest.Builder(this).data(url).size(512).allowHardware(false).build()
        val bitmap = (imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        if (url == artUrl && bitmap != null) {
            art = bitmap
            updateSession(latest)
            refreshNotification()
        }
    }

    private fun updateSession(ui: NowPlayingUi) {
        val pb = ui.playback
        if (pb == null) {
            metadataKey = null
            session.setPlaybackState(
                SessionPlaybackState.Builder().setState(SessionPlaybackState.STATE_NONE, 0L, 0f).build()
            )
            return
        }
        // Only touch metadata when it changes; the system UI re-renders on every call.
        val key = listOf(pb.title, pb.subtitle, pb.album, pb.durationMs, artUrl, art != null)
        if (key != metadataKey) {
            metadataKey = key
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, pb.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, pb.subtitle)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, pb.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, pb.durationMs)
                    .apply { art?.let { putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) } }
                    .build()
            )
        }
        // Position + timestamp lets the system animate the progress bar between polls.
        session.setPlaybackState(
            SessionPlaybackState.Builder()
                .setActions(ACTIONS)
                .setState(
                    if (pb.isPlaying) SessionPlaybackState.STATE_PLAYING else SessionPlaybackState.STATE_PAUSED,
                    pb.progressMs,
                    if (pb.isPlaying) 1f else 0f,
                    ui.syncedAt,
                )
                .build()
        )
    }

    private fun refreshNotification() {
        val pb = latest.playback
        val key = listOf(pb?.title, pb?.subtitle, pb?.isPlaying, artUrl, art != null)
        if (key == postedKey) return
        postedKey = key
        manager.notify(NOTIFICATION_ID, buildNotification())
        WidgetUpdater.update(this, pb, art)
    }

    private fun buildNotification(): Notification {
        val pb: PlaybackState? = latest.playback
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent())
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)

        if (pb == null) {
            return builder
                .setContentTitle("Serenity")
                .setContentText("Nothing playing")
                .addAction(action(R.drawable.ic_close, "Close", ACTION_STOP))
                .build()
        }

        art?.let { builder.setLargeIcon(it) }
        return builder
            .setContentTitle(pb.title)
            .setContentText(pb.subtitle)
            .addAction(action(R.drawable.ic_skip_previous, "Previous", ACTION_PREVIOUS))
            .addAction(
                action(
                    if (pb.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (pb.isPlaying) "Pause" else "Play",
                    ACTION_TOGGLE,
                )
            )
            .addAction(action(R.drawable.ic_skip_next, "Next", ACTION_NEXT))
            .addAction(action(R.drawable.ic_close, "Close", ACTION_STOP))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun action(iconRes: Int, title: String, action: String): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(this, iconRes), title, commandIntent(this, action)).build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Stop ourselves after a while with nothing to control (nothing playing, or paused for long). */
    private fun updateIdleTimer(pb: PlaybackState?) {
        if (pb?.isPlaying == true) {
            idleJob?.cancel()
            idleJob = null
            return
        }
        if (idleJob?.isActive == true) return
        val timeout = if (pb == null) NO_DEVICE_STOP_MS else PAUSED_STOP_MS
        idleJob = scope.launch {
            delay(timeout)
            stopSelf()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "io.github.serenity_player.serenity.action.TOGGLE"
        const val ACTION_NEXT = "io.github.serenity_player.serenity.action.NEXT"
        const val ACTION_PREVIOUS = "io.github.serenity_player.serenity.action.PREVIOUS"
        const val ACTION_STOP = "io.github.serenity_player.serenity.action.STOP"

        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
        private const val POLL_HOLDER = "service"
        private const val READY_TIMEOUT_MS = 5_000L
        private const val NO_DEVICE_STOP_MS = 2 * 60_000L
        private const val PAUSED_STOP_MS = 10 * 60_000L

        private const val ACTIONS = SessionPlaybackState.ACTION_PLAY or
            SessionPlaybackState.ACTION_PAUSE or
            SessionPlaybackState.ACTION_PLAY_PAUSE or
            SessionPlaybackState.ACTION_SKIP_TO_NEXT or
            SessionPlaybackState.ACTION_SKIP_TO_PREVIOUS or
            SessionPlaybackState.ACTION_SEEK_TO or
            SessionPlaybackState.ACTION_STOP

        /** Call while the app is visible; Android only allows starting a foreground service from there. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MediaPlaybackService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MediaPlaybackService::class.java))
        }

        /** For notification buttons and widget taps; also (re)starts the service if it isn't running. */
        fun commandIntent(context: Context, action: String): PendingIntent = PendingIntent.getForegroundService(
            context,
            0,
            Intent(context, MediaPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
