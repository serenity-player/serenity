package io.github.serenity_player.serenity.media

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home-screen widget: art, title, artist and previous / play-pause / next. Updated by [MediaPlaybackService]. */
class NowPlayingWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.update(context, WidgetUpdater.lastPlayback, WidgetUpdater.lastArt)
    }
}
