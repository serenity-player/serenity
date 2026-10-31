package io.github.serenity_player.serenity

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.github.serenity_player.serenity.auth.AuthManager
import io.github.serenity_player.serenity.auth.TokenStore
import io.github.serenity_player.serenity.data.api.SpotifyApiFactory
import io.github.serenity_player.serenity.data.repository.LibraryRepository
import io.github.serenity_player.serenity.data.repository.PlaybackRepository
import io.github.serenity_player.serenity.data.settings.SettingsStore
import io.github.serenity_player.serenity.player.NowPlayingViewModel

/** Tiny service locator. Swap for Hilt/Koin later if the app grows. */
class SerenityApp : Application(), ViewModelStoreOwner {
    // An app-level store, so the player state outlives any single activity.
    override val viewModelStore = ViewModelStore()

    val settings by lazy { SettingsStore(this) }
    val auth by lazy { AuthManager(TokenStore(this), settings) }
    val spotifyApi by lazy { SpotifyApiFactory.create(auth) }
    val playbackRepository by lazy { PlaybackRepository(spotifyApi) }
    val libraryRepository by lazy { LibraryRepository(spotifyApi) }

    /** Shared by the UI and the notification service, so both always see the same playback state. */
    val nowPlaying: NowPlayingViewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this))[NowPlayingViewModel::class.java]
    }
}
