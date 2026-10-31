package io.github.serenity_player.serenity.media

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import io.github.serenity_player.serenity.MainActivity
import io.github.serenity_player.serenity.R
import io.github.serenity_player.serenity.player.PlaybackState

object WidgetUpdater {
    // Last state pushed, so a freshly added widget isn't blank while the service is running.
    var lastPlayback: PlaybackState? = null
        private set
    var lastArt: Bitmap? = null
        private set

    fun update(context: Context, pb: PlaybackState?, art: Bitmap?) {
        lastPlayback = pb
        lastArt = art
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(context, pb, art))
    }

    private fun build(context: Context, pb: PlaybackState?, art: Bitmap?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_now_playing)
        views.setTextViewText(R.id.widget_title, pb?.title ?: "Serenity")
        views.setTextViewText(R.id.widget_subtitle, pb?.subtitle ?: "Nothing playing. Tap to open.")

        if (art != null) {
            // RemoteViews have a size limit on bitmaps, so shrink the art.
            views.setImageViewBitmap(R.id.widget_art, Bitmap.createScaledBitmap(art, 256, 256, true))
        } else {
            views.setImageViewResource(R.id.widget_art, R.drawable.ic_widget_placeholder)
        }
        views.setImageViewResource(
            R.id.widget_play,
            if (pb?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
        )

        views.setOnClickPendingIntent(R.id.widget_prev, MediaPlaybackService.commandIntent(context, MediaPlaybackService.ACTION_PREVIOUS))
        views.setOnClickPendingIntent(R.id.widget_play, MediaPlaybackService.commandIntent(context, MediaPlaybackService.ACTION_TOGGLE))
        views.setOnClickPendingIntent(R.id.widget_next, MediaPlaybackService.commandIntent(context, MediaPlaybackService.ACTION_NEXT))

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(android.R.id.background, open)
        return views
    }
}
